# LogNews

LogNews is a native Android news reader built with Kotlin.
It fetches top headlines from [NewsAPI.org](https://newsapi.org), includes a Bangla feed from Google News RSS, and keeps stories available with offline caching.

## Features

- **Top headlines feed** powered by NewsAPI Top Headlines endpoint
- **Category filters** for Business, Technology, and Bangla
- **Bangla headlines** from a Bangladesh-focused Google News RSS query without an extra API key
- **Pull-to-refresh** for loading the latest stories
- **Offline cache** for previously loaded headlines (stored per category)
- **Saved stories** list stored on device
- **In-app article reader** for every story, with an explicit “Read original article” action
- **Animated LogNews Brief** with multi-point local summarization generated from the cached publisher description, including offline
- **Editorial launch animation** and overflow menu for refresh, saved stories, and replaying the tour
- **Light onboarding flow** shown on first launch
- **Graceful error handling** for missing API keys, invalid responses, network issues, and timeouts

## Tech Stack

- **Language:** Kotlin
- **Platform:** Android (minSdk 24, target/compileSdk 35)
- **UI:** Android Views + View Binding + Material Components
- **Async work:** Kotlin Coroutines
- **Local storage:** SharedPreferences (JSON payloads)
- **Build system:** Gradle (Kotlin DSL), Android Gradle Plugin 8.6.1
- **CI:** GitHub Actions (build debug APK artifact)

## Project Structure

```text
LogNews/
├── app/
│   ├── src/main/java/com/example/newsreader/
│   │   ├── MainActivity.kt      # Screen logic, onboarding, category switching, refresh flow
│   │   ├── NewsApiClient.kt     # NewsAPI client + Bangla RSS parsing
│   │   ├── NewsCache.kt         # Cached and saved article persistence
│   │   ├── NewsAdapter.kt       # RecyclerView adapter for article cards
│   │   └── NewsArticle.kt       # Article data model
│   └── src/main/res/layout/
│       ├── activity_main.xml    # Main screen layout
│       └── item_news.xml        # News item layout
└── .github/workflows/build-android.yml
```

## Prerequisites

1. A NewsAPI key from [https://newsapi.org](https://newsapi.org)
2. One of the following:
   - **GitHub Actions build path** (recommended if local Android setup is unavailable), or
   - Local Android/Gradle environment with Java 17

## API Key Configuration

The app reads the key from the `NEWS_API_KEY` environment variable at build time and injects it into `BuildConfig.NEWS_API_KEY`.

### Option A: GitHub Actions (no local Android setup required)

1. Go to repository settings:
   - **Settings → Secrets and variables → Actions**
2. Create a new repository secret named:
   - `NEWS_API_KEY`
3. Set the value to your NewsAPI key.

### Option B: Local Build

Set `NEWS_API_KEY` in your shell before building:

```bash
export NEWS_API_KEY="your_newsapi_key_here"
```

## Build and Run

### Build with GitHub Actions

1. Open **Actions** tab.
2. Run workflow: **Build Android APK**.
3. Download artifact: `news-reader-debug-apk`.
4. Install `app-debug.apk` on your Android device.

> If you created or changed `NEWS_API_KEY`, reinstall an APK from a **new** workflow run.
> Older APKs built without the key will keep showing: `NEWS_API_KEY is missing`.

No Android SDK, Gradle, or Kotlin installation is required on your local machine when using this workflow.

### Build Locally

From repository root:

```bash
./gradlew assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## How the App Works

1. App starts and initializes cache, adapter, and onboarding.
2. Cached headlines for the selected category are shown if available.
3. Fresh headlines are requested from NewsAPI (or Bangla RSS for Bangla category).
4. Successful results are rendered and saved offline.
5. Users can:
   - switch categories,
   - save/unsave stories,
   - read every story in-app first,
   - request an animated local summary,
   - open the full article only through the explicit original-link button,
   - view saved stories and summaries offline.

## Error and Offline Behavior

- Missing API key: explicit message (`NEWS_API_KEY is missing`)
- Timeout and network failures: user-friendly retry guidance
- Invalid/empty API responses: handled with fallback errors
- If network fails after content is loaded, cached stories stay visible

## CI Workflow Summary

The workflow in `.github/workflows/build-android.yml`:

- checks out source
- sets up Java 17 and Android SDK
- validates that `NEWS_API_KEY` secret exists
- builds debug APK (`assembleDebug`)
- uploads APK as artifact (`news-reader-debug-apk`)

## Contribution Notes

If you are preparing a pull request from your branch (for example, `mahmudullah`):

1. Update code/docs
2. Commit your changes
3. Push your branch
4. Open a PR to the target branch

## License

No license file is currently included in this repository. Add one if you plan to distribute or open-source the project publicly.
