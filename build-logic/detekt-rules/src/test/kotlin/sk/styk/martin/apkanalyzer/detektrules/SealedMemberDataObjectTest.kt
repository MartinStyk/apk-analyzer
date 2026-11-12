package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class SealedMemberDataObjectTest {

    private val rule = SealedMemberDataObject(Config.empty)

    @Test
    fun `Given a plain object implementing a sealed interface When linting Then it is reported`() {
        val code = """
            sealed interface SettingsAction {
                object NavigateBack : SettingsAction
            }

            object Refresh : SettingsAction
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 2
        findings.first().message shouldContain "Declare 'NavigateBack' as a data object"
    }

    @Test
    fun `Given data objects and unrelated objects When linting Then nothing is reported`() {
        val code = """
            sealed interface SettingsAction {
                data object NavigateBack : SettingsAction

                companion object
            }

            object InstantSerializer : KSerializer<Instant>
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
