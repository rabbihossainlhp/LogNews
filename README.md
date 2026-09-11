# LogNews

LogNews is a native Android news reader built with Kotlin and Material 3. It fetches top U.S. headlines from [NewsAPI](https://newsapi.org/), caches them for offline reading, and lets users save stories for later.

## Features

- Top headlines feed powered by NewsAPI
- Category switching (Business and Technology)
- Pull-to-refresh updates
- Offline cache fallback for previously loaded stories
- Save/unsave stories and view a dedicated **Saved** feed
- First-launch onboarding card
- In-app error and retry states

## Tech Stack

- **Language:** Kotlin
- **UI:** Android Views + View Binding + Material 3
- **Concurrency:** Kotlin Coroutines
- **Networking:** `HttpURLConnection`
- **Storage:** `SharedPreferences` (JSON-encoded cached and saved articles)
- **Build:** Gradle Kotlin DSL, Android Gradle Plugin

## Project Structure

- `/home/runner/work/LogNews/LogNews/app/src/main/java/com/example/newsreader/MainActivity.kt` – app entry point, UI state, onboarding, category and saved-story handling
- `/home/runner/work/LogNews/LogNews/app/src/main/java/com/example/newsreader/NewsApiClient.kt` – NewsAPI request and response parsing
- `/home/runner/work/LogNews/LogNews/app/src/main/java/com/example/newsreader/NewsCache.kt` – local caching and saved-story persistence
- `/home/runner/work/LogNews/LogNews/app/src/main/java/com/example/newsreader/NewsAdapter.kt` – RecyclerView binding for article cards
- `/home/runner/work/LogNews/LogNews/app/src/main/java/com/example/newsreader/NewsArticle.kt` – article data model
- `/home/runner/work/LogNews/LogNews/app/src/main/res/layout/activity_main.xml` – main screen layout
- `/home/runner/work/LogNews/LogNews/.github/workflows/build-android.yml` – CI workflow that builds and uploads a debug APK

## Requirements

- Android Studio (latest stable recommended)
- Android SDK 35
- Java 17
- A NewsAPI key from https://newsapi.org/

## Configuration

The app reads the API key from the `NEWS_API_KEY` environment variable at build time and places it into `BuildConfig`.

### macOS / Linux

```bash
export NEWS_API_KEY="your_newsapi_key"
```

### Windows (PowerShell)

```powershell
$env:NEWS_API_KEY="your_newsapi_key"
```

## Build and Run Locally

From the repository root (`/home/runner/work/LogNews/LogNews`):

```bash
./gradlew assembleDebug
```

Debug APK output:

`/home/runner/work/LogNews/LogNews/app/build/outputs/apk/debug/app-debug.apk`

Then install the APK on an emulator or device.

## CI Build (GitHub Actions)

This repository includes a **Build Android APK** workflow.

1. Add repository secret: `NEWS_API_KEY`
2. Run the workflow from the **Actions** tab (or on push/PR)
3. Download artifact: `news-reader-debug-apk`

If the secret is missing, the workflow fails intentionally with setup instructions.

## App Behavior Notes

- If the API key is missing, the app reports `NEWS_API_KEY is missing`.
- If network calls fail and cached data exists, LogNews shows cached headlines in offline mode.
- Saved stories are persisted locally and can be accessed from the **Saved** tab.

## License

No license file is currently included in this repository.
