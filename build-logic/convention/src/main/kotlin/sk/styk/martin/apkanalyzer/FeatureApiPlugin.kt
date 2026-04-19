package sk.styk.martin.apkanalyzer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import sk.styk.martin.apkanalyzer.utils.api
import sk.styk.martin.apkanalyzer.utils.libs

class FeatureApiPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("apkanalyzer.library")
            apply("org.jetbrains.kotlin.plugin.serialization")
        }

        dependencies {
            api(libs.findLibrary("androidx-navigation3-runtime").get())
        }
    }
}
