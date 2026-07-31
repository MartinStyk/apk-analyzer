package sk.styk.martin.apkanalyzer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class AgentContextPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            require(this == rootProject) {
                "apkanalyzer.agent-context must be applied to the root project."
            }

            tasks.register<ValidateAgentContextTask>("validateAgentContext") {
                repositoryDirectory.set(layout.projectDirectory)
                modules.set(
                    subprojects
                        .filter { module -> module.buildFile.isFile }
                        .associate { module ->
                            module.path to module.projectDir.relativeTo(rootDir).invariantSeparatorsPath
                        },
                )
            }
        }
    }
}
