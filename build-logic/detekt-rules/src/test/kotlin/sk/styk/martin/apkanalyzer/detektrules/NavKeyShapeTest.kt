package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class NavKeyShapeTest {

    private val rule = NavKeyShape(Config.empty)

    @Test
    fun `Given a serializable data class and data object key When linting Then nothing is reported`() {
        val code = """
            @Serializable
            data class AppDetailKey(val packageName: String) : NavKey

            @Serializable
            data object SettingsKey : NavKey
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a key without Serializable When linting Then it is reported`() {
        val code = """
            data object SettingsKey : NavKey
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Annotate NavKey 'SettingsKey' with @Serializable"
    }

    @Test
    fun `Given a plain class key When linting Then it is reported`() {
        val code = """
            @Serializable
            class AppDetailKey(val packageName: String) : NavKey
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "NavKey 'AppDetailKey' is a data class"
    }

    @Test
    fun `Given a plain object key When linting Then it is reported`() {
        val code = """
            @Serializable
            object SettingsKey : NavKey
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "NavKey 'SettingsKey' is a data class"
    }

    @Test
    fun `Given a private key and a sealed key hierarchy When linting Then nothing is reported`() {
        val code = """
            private class PreviewKey : NavKey

            sealed interface TopLevelKey : NavKey

            sealed class TabKey : NavKey
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
