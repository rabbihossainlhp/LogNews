package com.example.newsreader

data class NewsArticle(
    val title: String,
    val description: String?,
    val source: String,
    val publishedAt: String?,
    val imageUrl: String?,
    val url: String
)
