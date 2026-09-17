package sk.styk.martin.apkanalyzer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import sk.styk.martin.apkanalyzer.utils.implementation
import sk.styk.martin.apkanalyzer.utils.ksp
import sk.styk.martin.apkanalyzer.utils.libs

class WorkPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")

        dependencies {
            implementation(libs.findLibrary("androidx.work.runtime.ktx").get())
            implementation(libs.findLibrary("androidx.hilt.work").get())
            ksp(libs.findLibrary("androidx.hilt.compiler").get())
        }
    }
}
