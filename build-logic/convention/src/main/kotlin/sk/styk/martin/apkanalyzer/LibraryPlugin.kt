package sk.styk.martin.apkanalyzer

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import sk.styk.martin.apkanalyzer.utils.CompileSdk
import sk.styk.martin.apkanalyzer.utils.MinSdk
import sk.styk.martin.apkanalyzer.utils.configureKotlin

class LibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
            apply("apkanalyzer.spotless")
        }

        extensions.configure<LibraryExtension> {
            compileSdk = CompileSdk
            defaultConfig {
                minSdk = MinSdk
            }
        }

        configureKotlin()
    }
}
