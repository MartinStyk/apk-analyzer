package sk.styk.martin.apkanalyzer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import sk.styk.martin.apkanalyzer.utils.implementation

class FeatureImplPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("apkanalyzer.library")
            apply("apkanalyzer.hilt")
            apply("apkanalyzer.compose")
        }

        dependencies {
            implementation(project(":core:ui-library"))
            implementation(project(":core:navigation"))
        }

    }
}
