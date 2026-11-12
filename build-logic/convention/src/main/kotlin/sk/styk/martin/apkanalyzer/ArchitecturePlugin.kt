package sk.styk.martin.apkanalyzer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class ArchitecturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            require(this == rootProject) {
                "apkanalyzer.architecture must be applied to the root project."
            }

            val modulesByPath = provider {
                subprojects
                    .filter { module -> module.buildFile.isFile }
                    .associate { module -> module.path to module.projectDir.relativeTo(rootDir).invariantSeparatorsPath }
            }

            tasks.register<ValidateArchitectureTask>("validateArchitecture") {
                repositoryDirectory.set(layout.projectDirectory)
                modules.set(modulesByPath)
            }

            tasks.register<ValidateTranslationsTask>("validateTranslations") {
                repositoryDirectory.set(layout.projectDirectory)
                modules.set(modulesByPath)
            }
        }
    }
}
