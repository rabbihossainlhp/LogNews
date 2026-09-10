package com.example.newsreader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.newsreader.databinding.ItemNewsBinding
import java.text.SimpleDateFormat
import java.util.Locale

class NewsAdapter(private val onArticleClick: (NewsArticle) -> Unit) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    private val articles = mutableListOf<NewsArticle>()

    fun submitList(newArticles: List<NewsArticle>) {
        articles.clear()
        articles.addAll(newArticles)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder = NewsViewHolder(
        ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) = holder.bind(articles[position])

    override fun getItemCount(): Int = articles.size

    inner class NewsViewHolder(private val binding: ItemNewsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(article: NewsArticle) {
            binding.title.text = article.title
            binding.description.text = article.description.orEmpty()
            binding.description.visibility = if (article.description.isNullOrBlank()) ViewGroup.GONE else ViewGroup.VISIBLE
            binding.source.text = listOfNotNull(article.source, article.publishedAt?.let(::formatDate)).joinToString("  •  ")
            binding.root.setOnClickListener { onArticleClick(article) }
        }

        private fun formatDate(value: String): String = try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val output = SimpleDateFormat("MMM d, yyyy", Locale.US)
            output.format(input.parse(value) ?: return value)
        } catch (_: Exception) {
            value.take(10)
        }
    }
}
