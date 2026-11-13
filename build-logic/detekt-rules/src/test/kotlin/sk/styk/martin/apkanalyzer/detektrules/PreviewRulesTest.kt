package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class PreviewRulesTest {

    private val previewShape = PreviewShape(Config.empty)
    private val previewPresence = PreviewPresence(Config.empty)

    @Test
    fun `Given a private themed preview When linting shape Then nothing is reported`() {
        val code = """
            @Preview
            @Composable
            private fun SettingsContentPreview() {
                ApkAnalyzerTheme {
                    SettingsContent(state = SettingsState())
                }
            }
        """.trimIndent()

        val findings = previewShape.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a preview wrapped in the TV theme When linting shape Then nothing is reported`() {
        val code = """
            @Preview(device = Devices.TV_1080p)
            @Composable
            private fun TvAppListPreview() {
                ApkAnalyzerTvTheme {
                    TvAppList(apps = listOf("WhatsApp"))
                }
            }
        """.trimIndent()

        val findings = previewShape.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a preview without the Preview suffix When linting shape Then it is reported`() {
        val code = """
            @PreviewLightDark
            @Composable
            private fun SettingsContentSample() {
                ApkAnalyzerTheme { SettingsContent(state = SettingsState()) }
            }
        """.trimIndent()

        val findings = previewShape.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Suffix preview 'SettingsContentSample' with Preview"
    }

    @Test
    fun `Given a preview without the app theme When linting shape Then it is reported`() {
        val code = """
            @Preview
            @Composable
            private fun SettingsContentPreview() {
                MaterialTheme { SettingsContent(state = SettingsState()) }
            }
        """.trimIndent()

        val findings = previewShape.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Wrap preview 'SettingsContentPreview' in ApkAnalyzerTheme"
    }

    @Test
    fun `Given a public composable without a preview When linting presence Then it is reported`() {
        val code = """
            @Composable
            fun SettingsContent(state: SettingsState, modifier: Modifier = Modifier) = Unit
        """.trimIndent()

        val findings = previewPresence.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Add a private @Preview"
    }

    @Test
    fun `Given a public composable with a PreviewLightDark preview When linting presence Then nothing is reported`() {
        val code = """
            @Composable
            fun SettingsContent(state: SettingsState, modifier: Modifier = Modifier) = Unit

            @PreviewLightDark
            @Composable
            private fun SettingsContentPreview() {
                ApkAnalyzerTheme { SettingsContent(state = SettingsState()) }
            }
        """.trimIndent()

        val findings = previewPresence.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given private scoped and remember composables without a preview When linting presence Then nothing is reported`() {
        val code = """
            @Composable
            private fun SettingsRow(title: String) = Unit

            @Composable
            fun RowScope.StatusBadge(label: String) = Unit

            @Composable
            fun rememberSettingsState(): SettingsState = SettingsState()
        """.trimIndent()

        val findings = previewPresence.lint(code)

        findings.shouldBeEmpty()
    }
}
