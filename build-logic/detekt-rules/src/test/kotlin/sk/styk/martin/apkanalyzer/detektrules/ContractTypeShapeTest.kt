package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class ContractTypeShapeTest {

    private val rule = ContractTypeShape(Config.empty)

    @Test
    fun `Given sealed interface Action and Event and an Immutable data class State When linting Then nothing is reported`() {
        val code = """
            sealed interface SettingsAction {
                data object NavigateBack : SettingsAction
            }

            sealed interface SettingsEvent {
                data object NavigateBack : SettingsEvent
            }

            @Immutable
            data class SettingsState(
                val languages: List<String> = emptyList(),
                val selected: Set<String> = emptySet(),
            )
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given an Action declared as a data class When linting Then it is reported`() {
        val code = """
            data class SettingsAction(val id: String)
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Declare 'SettingsAction' as a sealed interface"
    }

    @Test
    fun `Given an Event declared as a sealed class When linting Then it is reported`() {
        val code = """
            sealed class SettingsEvent {
                data object NavigateBack : SettingsEvent()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Declare 'SettingsEvent' as a sealed interface"
    }

    @Test
    fun `Given an analytics event class When linting Then nothing is reported`() {
        val code = """
            data class AppDetailAnalyticsEvent(val name: String)
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a State data class without Immutable When linting Then it is reported`() {
        val code = """
            data class SettingsState(val loading: Boolean)
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Declare 'SettingsState' as a sealed interface or an @Immutable data class"
    }

    @Test
    fun `Given an enum State When linting Then nothing is reported`() {
        val code = """
            enum class LoadState { Loading, Loaded }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a State holding a nullable lambda When linting Then it is reported`() {
        val code = """
            @Immutable
            data class SettingsState(val onRetry: (() -> Unit)?)
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "State never holds lambdas"
    }

    @Test
    fun `Given a sealed State whose nested variant holds an ArrayList When linting Then it is reported`() {
        val code = """
            sealed interface AppsState {
                data object Loading : AppsState

                @Immutable
                data class Loaded(val apps: ArrayList<String>) : AppsState
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "State holds read-only List, Set and Map only"
    }
}
