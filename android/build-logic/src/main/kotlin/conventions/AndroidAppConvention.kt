package conventions

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidAppConvention : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.android")

        extensions.configure<ApplicationExtension> {
            compileSdk = 36
            defaultConfig {
                minSdk = 24
                targetSdk = 36
                vectorDrawables { useSupportLibrary = true }
                versionCode = 1
                versionName = "0.1.0"
            }
            buildFeatures { compose = true }
            compileOptions {
                sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
            }
            packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
