plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("com.google.devtools.ksp")
  id("org.jetbrains.kotlin.plugin.serialization")
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

  buildFeatures {
    compose = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }

  buildTypes {
    debug {
      isMinifyEnabled = false
    }
    release {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
}

dependencies {
  // Compose BOM
  implementation(platform("androidx.compose:compose-bom:2024.10.01"))

  // Compose
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.animation:animation")
  implementation("androidx.compose.animation:animation-core")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")

  // AndroidX core libs
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.navigation:navigation-compose:2.8.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")

  // Material & datastore
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.datastore:datastore-preferences:1.1.1")

  // Room
  implementation("androidx.room:room-ktx:2.6.1")
  ksp("androidx.room:room-compiler:2.6.1")

  // Serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")

  // USB serial + coroutines
  implementation("com.hoho.android.usbserial:usb-serial-for-android:3.7.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

  // Tests
  testImplementation("junit:junit:4.13.2")
}
