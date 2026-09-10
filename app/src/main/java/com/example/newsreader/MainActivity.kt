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
    private companion object {
        const val TOUR_PREFERENCES = "lognews_preferences"
        const val TOUR_COMPLETED = "tour_completed"
    }

    private lateinit var binding: ActivityMainBinding
    private val apiClient = NewsApiClient()
    private lateinit var adapter: NewsAdapter
    private var tourPage = 0

    private val tourPages = listOf(
        "Welcome to LogNews" to "Start with a focused view of the latest business headlines.",
        "Read at a glance" to "Scan the source, date, and summary, then tap a story to read the full article.",
        "Stay in control" to "Pull down to refresh whenever you want the newest stories in your feed."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = NewsAdapter(::openArticle)
        binding.newsList.layoutManager = LinearLayoutManager(this)
        binding.newsList.adapter = adapter
        binding.refreshLayout.setOnRefreshListener { loadNews() }
        binding.retryButton.setOnClickListener { loadNews() }
        setupOnboarding()
        loadNews()
    }

    private fun setupOnboarding() {
        val preferences = getSharedPreferences(TOUR_PREFERENCES, MODE_PRIVATE)
        if (preferences.getBoolean(TOUR_COMPLETED, false)) {
            binding.onboardingCard.visibility = View.GONE
            return
        }

        renderTourPage()
        binding.onboardingSkip.setOnClickListener { finishOnboarding() }
        binding.onboardingNext.setOnClickListener {
            if (tourPage == tourPages.lastIndex) {
                finishOnboarding()
            } else {
                tourPage += 1
                renderTourPage()
            }
        }
    }

    private fun renderTourPage() {
        val (title, message) = tourPages[tourPage]
        binding.onboardingCard.animate()
            .alpha(0.35f)
            .translationX(10f)
            .setDuration(90)
            .withEndAction {
                binding.onboardingTitle.text = title
                binding.onboardingMessage.text = message
                binding.onboardingNext.text = if (tourPage == tourPages.lastIndex) "Done" else "Next"
                binding.onboardingCard.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(180)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun finishOnboarding() {
        getSharedPreferences(TOUR_PREFERENCES, MODE_PRIVATE)
            .edit()
            .putBoolean(TOUR_COMPLETED, true)
            .apply()
        binding.onboardingCard.animate()
            .alpha(0f)
            .setDuration(180)
            .withEndAction { binding.onboardingCard.visibility = View.GONE }
            .start()
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
