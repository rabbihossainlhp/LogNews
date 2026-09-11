package com.example.newsreader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class NewsCache(context: Context) {
    private val preferences = context.getSharedPreferences("lognews_cache", Context.MODE_PRIVATE)

    fun saveArticles(articles: List<NewsArticle>, category: String) {
        val payload = JSONArray()
        articles.forEach { article ->
            payload.put(
                JSONObject().apply {
                    put("title", article.title)
                    put("description", article.description)
                    put("source", article.source)
                    put("publishedAt", article.publishedAt)
                    put("imageUrl", article.imageUrl)
                    put("url", article.url)
                }
            )
        }
        preferences.edit().putString("$KEY_ARTICLES-$category", payload.toString()).apply()
    }

    fun loadArticles(category: String): List<NewsArticle> {
        val raw = preferences.getString("$KEY_ARTICLES-$category", null) ?: return emptyList()
        return try {
            val payload = JSONArray(raw)
            buildList {
                for (index in 0 until payload.length()) {
                    val item = payload.getJSONObject(index)
                    add(
                        NewsArticle(
                            title = item.optString("title"),
                            description = item.optString("description").takeUnless { it == "null" || it.isBlank() },
                            source = item.optString("source", "Unknown source"),
                            publishedAt = item.optString("publishedAt").takeUnless { it == "null" || it.isBlank() },
                            imageUrl = item.optString("imageUrl").takeUnless { it == "null" || it.isBlank() },
                            url = item.optString("url")
                        )
                    )
                }
            }.filter { it.title.isNotBlank() && it.url.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savedUrls(): Set<String> = preferences.getStringSet(KEY_SAVED_URLS, emptySet()).orEmpty()

    fun saveArticle(article: NewsArticle) {
        val articles = loadSavedArticles().filterNot { it.url == article.url } + article
        preferences.edit().putString(KEY_SAVED_ARTICLES, encode(articles)).apply()
    }

    fun removeSavedArticle(url: String) {
        val articles = loadSavedArticles().filterNot { it.url == url }
        preferences.edit().putString(KEY_SAVED_ARTICLES, encode(articles)).apply()
    }

    fun loadSavedArticles(): List<NewsArticle> {
        val raw = preferences.getString(KEY_SAVED_ARTICLES, null) ?: return emptyList()
        return decode(raw)
    }

    fun toggleSaved(url: String): Set<String> {
        val updated = savedUrls().toMutableSet()
        if (!updated.add(url)) updated.remove(url)
        preferences.edit().putStringSet(KEY_SAVED_URLS, updated).apply()
        return updated
    }

    private fun encode(articles: List<NewsArticle>): String {
        val payload = JSONArray()
        articles.forEach { article ->
            payload.put(article.toJson())
        }
        return payload.toString()
    }

    private fun decode(raw: String): List<NewsArticle> = try {
        val payload = JSONArray(raw)
        buildList {
            for (index in 0 until payload.length()) add(payload.getJSONObject(index).toArticle())
        }.filter { it.title.isNotBlank() && it.url.isNotBlank() }
    } catch (_: Exception) {
        emptyList()
    }

    private fun NewsArticle.toJson() = JSONObject().apply {
        put("title", title)
        put("description", description)
        put("source", source)
        put("publishedAt", publishedAt)
        put("imageUrl", imageUrl)
        put("url", url)
    }

    private fun JSONObject.toArticle() = NewsArticle(
        title = optString("title"),
        description = optString("description").takeUnless { it == "null" || it.isBlank() },
        source = optString("source", "Unknown source"),
        publishedAt = optString("publishedAt").takeUnless { it == "null" || it.isBlank() },
        imageUrl = optString("imageUrl").takeUnless { it == "null" || it.isBlank() },
        url = optString("url")
    )

    private companion object {
        const val KEY_ARTICLES = "cached_articles"
        const val KEY_SAVED_URLS = "saved_urls"
        const val KEY_SAVED_ARTICLES = "saved_articles"
    }
}
