plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.inskin.app"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.inskin.app"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0"
  }

  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.10.01"))
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material3:material3:1.3.0")
  implementation("androidx.navigation:navigation-compose:2.8.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
}
