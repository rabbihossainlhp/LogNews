package com.example.newsreader

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection

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
            .appendQueryParameter("apiKey", BuildConfig.NEWS_API_KEY)
            .build()
        val connection = endpoint.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

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
            val response = JSONObject(responseBody)
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
        } catch (exception: Exception) {
            Result.failure(exception)
        } finally {
            connection.disconnect()
        }
    }
}
