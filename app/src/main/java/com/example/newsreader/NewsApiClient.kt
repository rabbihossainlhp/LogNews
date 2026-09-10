package com.example.newsreader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NewsApiClient {
    suspend fun topHeadlines(country: String = "us"): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        if (BuildConfig.NEWS_API_KEY.isBlank()) {
            return@withContext Result.failure(IllegalStateException("NEWS_API_KEY is missing"))
        }

        val endpoint = URL(
            "https://newsapi.org/v2/top-headlines?country=$country&pageSize=50&apiKey=${BuildConfig.NEWS_API_KEY}"
        )
        val connection = endpoint.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        try {
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val response = JSONObject(responseBody)
            if (response.optString("status") != "ok") {
                return@withContext Result.failure(Exception(response.optString("message", "News request failed")))
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
