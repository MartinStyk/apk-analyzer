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
                target(
                    "src/**/*.kt",
                    "build-logic/convention/src/**/*.kt",
                )
                ktlint()
                    .customRuleSets(listOf(composeRulesKtlint))
                    .editorConfigOverride(
                        mapOf(
                            "ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to "3",
                            "ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to "3",
                            "ktlint_compose_compositionlocal-allowlist" to "disabled",
                            "ktlint_compose_lambda-param-in-effect" to "disabled",
                            "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                        ),
                    )
            }
            kotlinGradle {
                target(
                    "*.gradle.kts",
                    "settings.gradle.kts",
                    "build-logic/*.gradle.kts",
                    "build-logic/**/*.gradle.kts",
                )
                ktlint()
            }
            format("misc") {
                target(
                    ".github/**/*.yml",
                    ".github/**/*.yaml",
                    ".gitignore",
                    ".idea/.gitignore",
                )
                trimTrailingWhitespace()
                endWithNewline()
            }
        }
    }
}
