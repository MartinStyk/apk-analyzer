package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class LoggerTagTest {

    private val rule = LoggerTag(Config.empty)

    @Test
    fun `Given a file level private const TAG When linting Then nothing is reported`() {
        val code = """
            private const val TAG = "AppDetailRepository"

            class AppDetailRepositoryImpl
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a TAG in a companion object When linting Then it is reported`() {
        val code = """
            class AppDetailRepositoryImpl {
                private companion object {
                    private const val TAG = "AppDetailRepository"
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "file-level `private const val TAG`"
    }

    @Test
    fun `Given a public file level TAG When linting Then it is reported`() {
        val code = """
            const val TAG = "AppDetailRepository"
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given a private file level TAG without const When linting Then it is reported`() {
        val code = """
            private val TAG = "AppDetailRepository"
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }
}
