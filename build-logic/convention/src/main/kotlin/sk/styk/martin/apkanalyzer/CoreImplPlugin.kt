package sk.styk.martin.apkanalyzer

import org.gradle.api.Plugin
import org.gradle.api.Project

class CoreImplPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("apkanalyzer.library")
            apply("apkanalyzer.hilt")
        }
    }
}
