package com.example.newsreader

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class NewsApiClient {
    suspend fun topHeadlines(
        country: String = "us",
        category: String = "business"
    ): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
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
}
