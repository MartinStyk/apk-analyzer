package sk.styk.martin.apkanalyzer

import com.android.build.api.dsl.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import sk.styk.martin.apkanalyzer.utils.COMPILE_SDK
import sk.styk.martin.apkanalyzer.utils.MIN_SDK
import sk.styk.martin.apkanalyzer.utils.TARGET_SDK
import sk.styk.martin.apkanalyzer.utils.configureKotlin

class BaselineProfilePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val baselineProfileManagedDeviceName = findProperty("baselineProfileManagedDeviceName") as? String
            ?: "pixel6ApiTarget"

        with(pluginManager) {
            apply("com.android.test")
            apply("androidx.baselineprofile")
            apply("apkanalyzer.spotless")
            apply("apkanalyzer.detekt")
        }

        extensions.configure<TestExtension> {
            compileSdk = COMPILE_SDK

            defaultConfig {
                minSdk = MIN_SDK
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            targetProjectPath = ":app"
            experimentalProperties["android.experimental.self-instrumenting"] = true

            testOptions {
                managedDevices {
                    localDevices.maybeCreate(baselineProfileManagedDeviceName).apply {
                        device = "Pixel 6"
                        apiLevel = TARGET_SDK
                        systemImageSource = "google"
                        testedAbi = "x86_64"
                    }
                }
            }
        }

        configureKotlin()
    }
}
