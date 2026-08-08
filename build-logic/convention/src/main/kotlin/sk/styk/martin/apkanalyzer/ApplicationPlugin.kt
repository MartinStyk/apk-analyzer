package sk.styk.martin.apkanalyzer

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import sk.styk.martin.apkanalyzer.utils.COMPILE_SDK
import sk.styk.martin.apkanalyzer.utils.MIN_SDK
import sk.styk.martin.apkanalyzer.utils.TARGET_SDK
import sk.styk.martin.apkanalyzer.utils.configureKotlin
import sk.styk.martin.apkanalyzer.utils.implementation
import sk.styk.martin.apkanalyzer.utils.libs

class ApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("com.google.gms.google-services")
            apply("com.google.firebase.firebase-perf")
            apply("com.google.firebase.crashlytics")
            apply("apkanalyzer.detekt")
        }

        extensions.configure<ApplicationExtension> {
            compileSdk = COMPILE_SDK
            defaultConfig {
                targetSdk = TARGET_SDK
                minSdk = MIN_SDK
            }

            signingConfigs {
                getByName("debug") {
                    storeFile = target.file("debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }

            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                }
                getByName("debug") {
                    isMinifyEnabled = false
                    isShrinkResources = false
                }
            }
        }

        configureKotlin()

        dependencies {
            val bom = libs.findLibrary("firebase-bom").get()
            implementation(platform(bom))
            implementation(libs.findLibrary("firebase.analytics").get())
            implementation(libs.findLibrary("firebase.performance").get())
            implementation(libs.findLibrary("firebase.crashlytics").get())
        }
    }
}
