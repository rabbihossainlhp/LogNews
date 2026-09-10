package com.example.newsreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newsreader.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val apiClient = NewsApiClient()
    private lateinit var adapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = NewsAdapter(::openArticle)
        binding.newsList.layoutManager = LinearLayoutManager(this)
        binding.newsList.adapter = adapter
        binding.refreshLayout.setOnRefreshListener { loadNews() }
        binding.retryButton.setOnClickListener { loadNews() }
        loadNews()
    }

    private fun loadNews() {
        binding.refreshLayout.isRefreshing = true
        binding.errorPanel.visibility = View.GONE
        if (adapter.itemCount == 0) binding.loading.visibility = View.VISIBLE

        lifecycleScope.launch {
            apiClient.topHeadlines().onSuccess { articles ->
                adapter.submitList(articles)
                binding.loading.visibility = View.GONE
                binding.emptyState.visibility = if (articles.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure { error ->
                binding.loading.visibility = View.GONE
                binding.emptyState.visibility = View.GONE
                binding.errorPanel.visibility = View.VISIBLE
                binding.errorMessage.text = error.message ?: "Could not load news."
            }
            binding.refreshLayout.isRefreshing = false
        }
    }

    private fun openArticle(article: NewsArticle) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open article", Toast.LENGTH_SHORT).show()
        }
    }
}
