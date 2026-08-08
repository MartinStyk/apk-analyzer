package sk.styk.martin.apkanalyzer

import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class DetektPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.detekt")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig.set(true)
            config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
            basePath.set(rootProject.layout.projectDirectory)
            parallel.set(true)
            failOnSeverity.set(FailOnSeverity.Error)
        }
    }
}
