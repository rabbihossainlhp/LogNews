# News Reader

A native Kotlin Android app that loads top headlines from NewsAPI.org.

## GitHub Actions

Create a repository secret named `NEWS_API_KEY` at **Settings -> Secrets and variables -> Actions -> New repository secret**, then paste your NewsAPI.org key as its value. Run the **Build Android APK** workflow and install the newly generated `news-reader-debug-apk` artifact on the phone.

The key is embedded into the APK at build time. Reinstall an APK from a workflow run made after the secret was created; an older APK built without the secret will continue to show `NEWS_API_KEY is missing`.

## Features

- Business and technology headlines from NewsAPI.org
- Bangla headlines from Google News RSS (`hl=bn`, Bangladesh edition), which does not require another API key
- Offline cache for the latest feed in each category
- Saved stories with an in-app summary card, available without internet

No Android SDK, Gradle, or Kotlin installation is required on the local machine.
