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
    fun `Given a plain object nested in its sealed interface When linting Then it is reported`() {
        val code = """
            sealed interface SettingsAction {
                object NavigateBack : SettingsAction
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Declare 'NavigateBack' as a data object"
    }

    @Test
    fun `Given a plain object implementing a sealed type declared elsewhere in the file When linting Then it is reported`() {
        val code = """
            sealed interface SettingsEvent

            object NavigateBack : SettingsEvent
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given data objects in a sealed interface When linting Then nothing is reported`() {
        val code = """
            sealed interface SettingsAction {
                data object NavigateBack : SettingsAction
                data class SelectLanguage(val tag: String) : SettingsAction
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given objects that are not sealed members When linting Then nothing is reported`() {
        val code = """
            sealed interface Callback

            object AppNameComparator : Comparator<String> {
                override fun compare(a: String, b: String) = a.compareTo(b)
            }

            class Holder {
                companion object {
                    const val KEY = "key"
                }
            }

            val callback = object : Callback {}
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
