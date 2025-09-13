plugins {
  id("android-app-convention")
  id("org.jetbrains.kotlin.plugin.compose")
}
android { namespace = "com.inskin.app" }

dependencies {
  implementation(project(":data"))
  implementation(project(":tags"))
  implementation(project(":usb"))

  // Compose: BOM => pas de versions pour les artefacts Compose
  implementation(platform("androidx.compose:compose-bom:2025.08.01"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.animation:animation")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")

  // Pas dans le BOM => versions explicites
  implementation("androidx.core:core-ktx:1.17.0")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.navigation:navigation-compose:2.9.3")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
  implementation("com.google.android.material:material:1.13.0")
  implementation("com.google.code.gson:gson:2.13.1")
  implementation("androidx.datastore:datastore-preferences:1.1.7")
}

