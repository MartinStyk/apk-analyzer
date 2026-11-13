package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class NoCommentsTest {

    private val rule = NoComments(Config.empty)

    @Test
    fun `Given a line comment in a function When linting Then it is reported`() {
        val code = """
            fun loadApps() {
                // load installed apps first
                val apps = emptyList<String>()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Comments and KDoc are not allowed"
    }

    @Test
    fun `Given KDoc on a plain function When linting Then it is reported`() {
        val code = """
            /**
             * Loads installed apps.
             */
            fun loadApps() = Unit
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given KDoc on an AppFunction When linting Then nothing is reported`() {
        val code = """
            class AppFunctions {
                /**
                 * Finds installed apps whose name matches the query.
                 */
                @AppFunction
                suspend fun findApps(context: AppFunctionContext, query: String): List<AppSummary> = emptyList()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given KDoc on a property of an AppFunctionSerializable class When linting Then nothing is reported`() {
        val code = """
            /**
             * An installed app.
             */
            @AppFunctionSerializable
            data class AppSummary(
                /** The app's package name. */
                val packageName: String,
            )
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a block comment inside an AppFunction body When linting Then it is reported`() {
        val code = """
            /**
             * Finds installed apps.
             */
            @AppFunction
            suspend fun findApps(query: String): List<String> {
                /* filter later */
                return emptyList()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }
}
