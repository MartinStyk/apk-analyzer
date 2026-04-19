package sk.styk.martin.apkanalyzer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import sk.styk.martin.apkanalyzer.utils.implementation
import sk.styk.martin.apkanalyzer.utils.libs

class ComposePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("org.jetbrains.kotlin.plugin.serialization")
        }

        dependencies {
            implementation(platform(libs.findLibrary("androidx.compose.bom").get()))
            implementation(libs.findBundle("compose").get())
            implementation(libs.findBundle("navigation3").get())
        }
    }
}
