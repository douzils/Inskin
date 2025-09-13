pluginManagement {
  repositories { google(); mavenCentral(); gradlePluginPortal() }
  plugins {
    id("com.android.application") version "8.13.0"
    id("com.android.library")     version "8.13.0"
    id("org.jetbrains.kotlin.android")        version "2.2.10"
    id("org.jetbrains.kotlin.jvm")            version "2.2.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.devtools.ksp")             version "2.2.10-2.0.2"
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories { google(); mavenCentral() }
}
rootProject.name = "Inskin"
includeBuild("build-logic")
include(":app", ":data", ":tags", ":usb")
