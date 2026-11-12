package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class NoKtlintSuppressionTest {

    private val rule = NoKtlintSuppression(Config.empty)

    @Test
    fun `Given a ktlint-disable comment When linting Then it is reported`() {
        val code = """
            import sk.styk.martin.apkanalyzer.core.apps.* // ktlint-disable no-wildcard-imports
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Don't suppress ktlint"
    }

    @Test
    fun `Given a Suppress annotation naming a ktlint rule When linting Then it is reported`() {
        val code = """
            @Suppress("ktlint:standard:max-line-length")
            fun loadApps() = Unit
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given a Suppress annotation for a compiler warning When linting Then nothing is reported`() {
        val code = """
            @Suppress("UNUSED_PARAMETER")
            fun loadApps(force: Boolean) = Unit
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
