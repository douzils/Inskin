plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

repositories {
  google()
  mavenCentral()
}

dependencies {
  implementation("com.android.tools.build:gradle:8.13.0")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
  implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.2.10-2.0.2")
}

gradlePlugin {
  plugins {
    register("androidAppConvention") {
      id = "android-app-convention"
      implementationClass = "conventions.AndroidAppConvention"
    }
    register("androidLibConvention") {
      id = "android-lib-convention"
      implementationClass = "conventions.AndroidLibConvention"
    }
    register("androidKspConvention") {
      id = "android-ksp-convention"
      implementationClass = "conventions.AndroidKspConvention"
    }
  }
}
