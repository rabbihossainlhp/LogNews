plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.newsreader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.newsreader"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val newsApiKey = providers.environmentVariable("NEWS_API_KEY").orNull ?: ""
        val escapedKey = newsApiKey.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "NEWS_API_KEY", "\"$escapedKey\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
}
