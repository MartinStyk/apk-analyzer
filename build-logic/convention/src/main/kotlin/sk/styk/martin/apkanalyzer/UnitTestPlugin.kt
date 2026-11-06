package sk.styk.martin.apkanalyzer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import sk.styk.martin.apkanalyzer.utils.androidLibrary
import sk.styk.martin.apkanalyzer.utils.libs
import sk.styk.martin.apkanalyzer.utils.testFixturesImplementation
import sk.styk.martin.apkanalyzer.utils.testImplementation

class UnitTestPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        androidLibrary.testFixtures {
            enable = true
        }

        dependencies {
            testImplementation(libs.findLibrary("junit").get())
            testImplementation(libs.findLibrary("kotest-assertions-core").get())
            testImplementation(libs.findLibrary("kotlinx-coroutines-test").get())
            testImplementation(libs.findLibrary("mockk").get())
            testFixturesImplementation(libs.findLibrary("junit").get())
            testFixturesImplementation(libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
