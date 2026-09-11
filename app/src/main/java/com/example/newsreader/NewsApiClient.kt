package com.example.newsreader

import android.net.Uri
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import org.xmlpull.v1.XmlPullParser

class NewsApiClient {
    suspend fun topHeadlines(
        country: String = "us",
        category: String = "business"
    ): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        if (category == "bangla") {
            return@withContext fetchBanglaHeadlines()
        }

        if (BuildConfig.NEWS_API_KEY.isBlank()) {
            return@withContext Result.failure(IllegalStateException("NEWS_API_KEY is missing"))
        }

        val endpoint = Uri.parse("https://newsapi.org/v2/top-headlines").buildUpon()
            .appendQueryParameter("country", country)
            .appendQueryParameter("category", category)
            .appendQueryParameter("pageSize", "50")
            .build()
        val connection = URL(endpoint.toString()).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("X-Api-Key", BuildConfig.NEWS_API_KEY)
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "LogNews/1.0")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.useCaches = false

        try {
            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseBody.isBlank()) {
                return@withContext Result.failure(Exception("News request failed (HTTP $responseCode)"))
            }
            val trimmedBody = responseBody.trimStart()
            if (!trimmedBody.startsWith("{")) {
                return@withContext Result.failure(
                    Exception("The news service returned an invalid response. Please try again later.")
                )
            }

            val response = try {
                JSONObject(trimmedBody)
            } catch (_: Exception) {
                return@withContext Result.failure(
                    Exception("The news service returned invalid data. Please try again later.")
                )
            }
            if (response.optString("status") != "ok") {
                val message = response.optString("message").ifBlank {
                    "News request failed (HTTP $responseCode)"
                }
                return@withContext Result.failure(Exception(message))
            }

            val articles = buildList {
                val items = response.getJSONArray("articles")
                for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    val title = item.optString("title").trim()
                    val url = item.optString("url").trim()
                    if (title.isNotEmpty() && url.isNotEmpty()) {
                        add(
                            NewsArticle(
                                title = title,
                                description = item.optString("description").takeUnless { it.isBlank() || it == "null" },
                                source = item.optJSONObject("source")?.optString("name") ?: "Unknown source",
                                publishedAt = item.optString("publishedAt").takeUnless { it.isBlank() || it == "null" },
                                imageUrl = item.optString("urlToImage").takeUnless { it.isBlank() || it == "null" },
                                url = url
                            )
                        )
                    }
                }
            }
            Result.success(articles)
        } catch (_: SocketTimeoutException) {
            Result.failure(Exception("The request timed out. Check your connection and try again."))
        } catch (_: IOException) {
            Result.failure(Exception("Unable to reach the news service. Check your connection and try again."))
        } catch (_: Exception) {
            Result.failure(Exception("Something went wrong while loading the news. Please try again."))
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchBanglaHeadlines(): Result<List<NewsArticle>> {
        val bangladeshFeed = Uri.parse("https://news.google.com/rss/search").buildUpon()
            .appendQueryParameter("q", "Bangladesh OR ঢাকা OR বাংলাদেশ")
            .appendQueryParameter("hl", "bn")
            .appendQueryParameter("gl", "BD")
            .appendQueryParameter("ceid", "BD:bn")
            .build()
        val connection = URL(bangladeshFeed.toString())
            .openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/rss+xml, application/xml")
        connection.setRequestProperty("User-Agent", "LogNews/1.0")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.useCaches = false

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Result.failure(Exception("Bangla news is unavailable right now (HTTP $responseCode)."))
            } else {
                connection.inputStream.use(::parseBanglaRss)
            }
        } catch (_: SocketTimeoutException) {
            Result.failure(Exception("The Bangla news request timed out. Try again later."))
        } catch (_: IOException) {
            Result.failure(Exception("Unable to reach the Bangla news service."))
        } catch (_: Exception) {
            Result.failure(Exception("Bangla news returned an invalid response."))
        } finally {
            connection.disconnect()
        }
    }

    private fun parseBanglaRss(input: InputStream): Result<List<NewsArticle>> {
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        val articles = mutableListOf<NewsArticle>()
        var event = parser.eventType
        var insideItem = false
        var title = ""
        var link = ""
        var description = ""
        var publishedAt = ""
        var source = ""
        var currentTag = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        insideItem = true
                        title = ""
                        link = ""
                        description = ""
                        publishedAt = ""
                        source = ""
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (insideItem) {
                    when (currentTag) {
                        "title" -> title += parser.text
                        "link" -> link += parser.text
                        "description" -> description += parser.text
                        "pubDate" -> publishedAt += parser.text
                        "source" -> source += parser.text
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "item") {
                    if (title.isNotBlank() && link.isNotBlank()) {
                        articles += NewsArticle(
                            title = title.trim(),
                            description = stripMarkup(description).takeUnless { it.isBlank() },
                            source = source.trim().ifBlank { "Bangla News" },
                            publishedAt = publishedAt.trim().takeUnless { it.isBlank() },
                            imageUrl = null,
                            url = link.trim()
                        )
                    }
                    insideItem = false
                    currentTag = ""
                }
            }
            event = parser.next()
        }
        return Result.success(articles.take(50))
    }

    private fun stripMarkup(value: String): String = value
        .replace(Regex("<[^>]*>"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
