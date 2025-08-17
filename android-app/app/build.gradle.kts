plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("com.google.devtools.ksp")
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

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  signingConfigs {
    // configured via environment variables
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.10.01"))
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material3:material3:1.3.0")
  implementation("androidx.navigation:navigation-compose:2.8.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
  implementation("androidx.compose.material:material-icons-extended:1.6.8")

  // Room + KSP
  implementation("androidx.room:room-ktx:2.6.1")
  ksp("androidx.room:room-compiler:2.6.1")

  // DataStore
  implementation("androidx.datastore:datastore-preferences:1.1.1")

  // Serialization for export JSON
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")

  testImplementation("junit:junit:4.13.2")
}
