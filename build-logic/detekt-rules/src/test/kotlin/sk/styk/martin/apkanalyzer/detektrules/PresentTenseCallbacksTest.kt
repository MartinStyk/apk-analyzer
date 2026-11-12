package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class PresentTenseCallbacksTest {

    private val rule = PresentTenseCallbacks(Config.empty)

    @Test
    fun `Given a past tense callback on a composable When linting Then it is reported`() {
        val code = """
            @Composable
            fun AppRow(name: String, onItemSelected: (String) -> Unit) = Unit
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Name the callback 'onItemSelected' in the present tense"
    }

    @Test
    fun `Given onClicked and onBackPressed on a composable When linting Then both are reported`() {
        val code = """
            @Composable
            fun SettingsContent(onClicked: () -> Unit, onBackPressed: () -> Unit) = Unit
        """.trimIndent()

        val findings = rule.lint(code)

        findings.map { it.message.contains("present tense") } shouldBe listOf(true, true)
    }

    @Test
    fun `Given present tense callbacks on a composable When linting Then nothing is reported`() {
        val code = """
            @Composable
            fun AppRow(
                onClick: () -> Unit,
                onSelectItem: (String) -> Unit,
                onBack: () -> Unit,
                onCheckedChange: (Boolean) -> Unit,
            ) = Unit
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a past tense callback on a regular function When linting Then nothing is reported`() {
        val code = """
            fun observeSelection(onItemSelected: (String) -> Unit) = Unit
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
