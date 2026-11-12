package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class AppFunctionKDocRequiredTest {

    private val rule = AppFunctionKDocRequired(Config.empty)

    @Test
    fun `Given an AppFunction without KDoc When linting Then it is reported`() {
        val code = """
            class AppFunctions {
                @AppFunction
                suspend fun findApps(query: String): List<String> = emptyList()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Document 'findApps' with KDoc"
    }

    @Test
    fun `Given an AppFunction with KDoc When linting Then nothing is reported`() {
        val code = """
            class AppFunctions {
                /**
                 * Finds installed apps whose name matches the query.
                 */
                @AppFunction
                suspend fun findApps(query: String): List<String> = emptyList()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given an AppFunctionSerializable class without KDoc When linting Then it is reported`() {
        val code = """
            @AppFunctionSerializable
            data class AppSummary(val packageName: String)
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Document 'AppSummary' with KDoc"
    }

    @Test
    fun `Given a plain function without KDoc When linting Then nothing is reported`() {
        val code = """
            suspend fun findApps(query: String): List<String> = emptyList()
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
