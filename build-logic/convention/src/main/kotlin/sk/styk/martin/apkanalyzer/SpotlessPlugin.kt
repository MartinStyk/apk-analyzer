package sk.styk.martin.apkanalyzer

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import sk.styk.martin.apkanalyzer.utils.libs

class SpotlessPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.diffplug.spotless")
        }

        val composeRulesKtlint = libs.findLibrary("compose-rules-ktlint").get()
            .let { "${it.get().module}:${it.get().versionConstraint.requiredVersion}" }

        extensions.configure<SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude("${layout.buildDirectory}/**/*.kt")
                targetExclude("bin/**/*.kt")
                ktlint()
                    .customRuleSets(listOf(composeRulesKtlint))
                    .editorConfigOverride(
                        mapOf(
                            "ktlint_compose_compositionlocal-allowlist" to "disabled",
                        ),
                    )
            }
            kotlinGradle {
                target("**/*.kt")
                target("**/*.kts")
                targetExclude("${layout.buildDirectory}/**/*.kts")
                ktlint()
            }
        }
    }
}
