package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class HardcodedComposeStringTest {

    private val rule = HardcodedComposeString(Config.empty)

    @Test
    fun `Given a literal as the first argument of a Text composable When linting Then it is reported`() {
        val code = """
            @Composable
            fun SettingsHeader() {
                ApkAnalyzerText("Settings")
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Move this user-facing text to res/values/strings.xml"
    }

    @Test
    fun `Given a literal content description When linting Then it is reported`() {
        val code = """
            @Composable
            fun CloseButton(onClick: () -> Unit) {
                IconButton(onClick = onClick, icon = ApkAnalyzerIcons.Close, contentDescription = "Close")
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given text from string resources and non text literals When linting Then nothing is reported`() {
        val code = """
            @Composable
            fun SettingsHeader() {
                ApkAnalyzerText(stringResource(R.string.settings_title))
                ApkAnalyzerText(" • ")
                AppRow(packageName = "com.whatsapp", modifier = Modifier.testTag("settings_header"))
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given literals in preview code and sample data When linting Then nothing is reported`() {
        val code = """
            @Preview
            @Composable
            private fun SettingsHeaderPreview() {
                ApkAnalyzerTheme {
                    ApkAnalyzerText(text = "Settings")
                }
            }

            private fun sampleApps() = listOf(AppRow(title = "WhatsApp"))
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a literal in a top level preview property When linting Then nothing is reported`() {
        val code = """
            private val previewContent = InventoryPreviewState.Content(text = "package_name,version_name")
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a debug label on an animation composable When linting Then nothing is reported`() {
        val code = """
            @Composable
            fun Panel(state: String) {
                Crossfade(targetState = state, label = "panelContent") { }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a literal label on a regular composable When linting Then it is reported`() {
        val code = """
            @Composable
            fun Filter() {
                Chip(label = "Installed", onClick = {})
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }
}
