package sk.styk.martin.apkanalyzer

import dev.detekt.gradle.report.ReportMergeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class SarifMergePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            require(this == rootProject) {
                "apkanalyzer.sarif-merge must be applied to the root project."
            }

            tasks.register<ReportMergeTask>("mergeSarifReports") {
                input.from(
                    subprojects.map { module ->
                        module.fileTree(module.layout.buildDirectory) {
                            include("reports/detekt/*.sarif", "reports/lint-results-*.sarif")
                        }
                    },
                )
                input.from(
                    fileTree(layout.projectDirectory.dir("build-logic")) {
                        include("**/build/reports/detekt/*.sarif")
                    },
                )
                output.set(layout.buildDirectory.file("reports/merged.sarif"))
            }
        }
    }
}
