package sk.styk.martin.apkanalyzer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import sk.styk.martin.apkanalyzer.utils.api
import sk.styk.martin.apkanalyzer.utils.libs

class CoreApiPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("apkanalyzer.library")
            apply("apkanalyzer.tests.unit")
            apply("org.jetbrains.kotlin.plugin.serialization")
        }

        dependencies {
            api(libs.findLibrary("kotlinx-coroutines-core").get())
            api(libs.findLibrary("kotlinx-serialization-core").get())
        }
    }
}
