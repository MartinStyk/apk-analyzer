package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class NoRawColorTest {

    private val rule = NoRawColor(Config.empty)

    @Test
    fun `Given a hex Color literal When linting Then it is reported`() {
        val code = """
            val warning = Color(0xFFFFA000)
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Use a color from AppTheme.colors"
    }

    @Test
    fun `Given a named Color constant When linting Then it is reported`() {
        val code = """
            @Composable
            fun Badge() {
                Box(modifier = Modifier.background(Color.Red))
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given theme colors Transparent and a computed Color When linting Then nothing is reported`() {
        val code = """
            @Composable
            fun Badge(argb: Int) {
                Box(modifier = Modifier.background(AppTheme.colors.primary))
                Box(modifier = Modifier.background(Color.Transparent))
                Box(modifier = Modifier.background(Color(argb)))
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
