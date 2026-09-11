package com.example.newsreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.newsreader.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private companion object {
        const val TOUR_PREFERENCES = "lognews_preferences"
        const val TOUR_COMPLETED = "tour_completed"
    }

    private lateinit var binding: ActivityMainBinding
    private val apiClient = NewsApiClient()
    private lateinit var cache: NewsCache
    private lateinit var adapter: NewsAdapter
    private var tourPage = 0
    private var currentCategory = "business"
    private var showingSaved = false
    private var savedUrls = emptySet<String>()

    private val tourPages = listOf(
        "Welcome to LogNews" to "Start with a focused view of the latest business headlines.",
        "Read at a glance" to "Scan the source, date, and summary, then tap a story to read the full article.",
        "Stay in control" to "Pull down to refresh whenever you want the newest stories in your feed."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cache = NewsCache(this)
        savedUrls = cache.savedUrls()
        adapter = NewsAdapter(::openArticle, { article -> article.url in savedUrls }, ::toggleSaved)
        binding.newsList.layoutManager = LinearLayoutManager(this)
        binding.newsList.adapter = adapter
        binding.refreshLayout.setOnRefreshListener { loadNews() }
        binding.retryButton.setOnClickListener { loadNews() }
        binding.businessButton.setOnClickListener { selectCategory("business") }
        binding.technologyButton.setOnClickListener { selectCategory("technology") }
        binding.banglaButton.setOnClickListener { selectCategory("bangla") }
        binding.savedButton.setOnClickListener { showSavedStories() }
        setupOnboarding()
        showCachedArticles()
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

    private fun selectCategory(category: String) {
        showingSaved = false
        currentCategory = category
        binding.feedSubtitle.text = when (category) {
            "bangla" -> "বাংলা সংবাদ, thoughtfully selected"
            else -> "$category headlines, thoughtfully selected"
        }
        showCachedArticles()
        loadNews()
    }

    private fun showSavedStories() {
        showingSaved = true
        val savedArticles = cache.loadSavedArticles().filter { it.url in savedUrls }
        renderArticles(savedArticles)
        binding.feedSubtitle.text = "Your saved stories, ready anytime"
        binding.feedStatus.text = if (savedArticles.isEmpty()) "Save a story to find it here, even offline." else "${savedArticles.size} saved ${if (savedArticles.size == 1) "story" else "stories"}"
        binding.feedStatus.visibility = View.VISIBLE
        binding.errorPanel.visibility = View.GONE
    }

    private fun toggleSaved(article: NewsArticle) {
        if (article.url in savedUrls) {
            cache.removeSavedArticle(article.url)
        } else {
            cache.saveArticle(article)
        }
        savedUrls = cache.toggleSaved(article.url)
        if (showingSaved) showSavedStories() else adapter.notifyDataSetChanged()
        Toast.makeText(this, if (article.url in savedUrls) "Story saved" else "Removed from saved", Toast.LENGTH_SHORT).show()
    }

    private fun showCachedArticles() {
        val cachedArticles = cache.loadArticles(currentCategory)
        if (cachedArticles.isNotEmpty()) {
            renderArticles(cachedArticles)
            binding.feedStatus.text = "Cached headlines available offline"
            binding.feedStatus.visibility = View.VISIBLE
        }
    }

    private fun renderArticles(articles: List<NewsArticle>) {
        adapter.submitList(articles)
        binding.loading.visibility = View.GONE
        binding.emptyState.visibility = if (articles.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadNews() {
        if (showingSaved) return
        binding.refreshLayout.isRefreshing = true
        binding.errorPanel.visibility = View.GONE
        if (adapter.itemCount == 0) binding.loading.visibility = View.VISIBLE

        lifecycleScope.launch {
            apiClient.topHeadlines(category = currentCategory).onSuccess { articles ->
                cache.saveArticles(articles, currentCategory)
                binding.feedStatus.text = "Updated just now • also available offline"
                binding.feedStatus.visibility = View.VISIBLE
                renderArticles(articles)
            }.onFailure { error ->
                binding.loading.visibility = View.GONE
                if (adapter.itemCount == 0) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.errorPanel.visibility = View.VISIBLE
                    binding.errorMessage.text = error.message ?: "Could not load news."
                } else {
                    binding.feedStatus.text = "Offline mode • showing your cached headlines"
                    binding.feedStatus.visibility = View.VISIBLE
                }
            }
            binding.refreshLayout.isRefreshing = false
        }
    }

    private fun openArticle(article: NewsArticle) {
        if (article.url in savedUrls) {
            showSavedSummary(article)
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open article", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSavedSummary(article: NewsArticle) {
        val summary = article.description?.takeIf { it.isNotBlank() }
            ?: "This saved story has no publisher summary. You can open the original article when you are online."
        MaterialAlertDialogBuilder(this)
            .setTitle(article.title)
            .setMessage("${article.source}\n\n$summary")
            .setPositiveButton("Close", null)
            .setNeutralButton("Open original") { _, _ -> openArticleOnline(article) }
            .show()
    }

    private fun openArticleOnline(article: NewsArticle) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open article", Toast.LENGTH_SHORT).show()
        }
    }
}
