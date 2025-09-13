plugins { id("android-lib-convention"); id("android-ksp-convention"); id("com.google.devtools.ksp") }
android { namespace = "com.inskin.data" }
dependencies {
  // Room (versions explicites)
  implementation("androidx.room:room-runtime:2.7.0")
  implementation("androidx.room:room-ktx:2.7.0")
  ksp("androidx.room:room-compiler:2.7.0")
}
ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.incremental", "true")
  arg("room.expandProjection", "true")
}
