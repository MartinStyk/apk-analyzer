package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class MutableFlowExposureTest {

    private val rule = MutableFlowExposure(Config.empty)

    @Test
    fun `Given a private mutable flow exposed with asStateFlow When linting Then the call is reported`() {
        val code = """
            class SettingsViewModel {
                private val mutableState = MutableStateFlow(0)
                val state: StateFlow<Int> = mutableState.asStateFlow()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "asStateFlow()"
    }

    @Test
    fun `Given a public MutableStateFlow initializer When linting Then it is reported`() {
        val code = """
            class SettingsViewModel {
                val state = MutableStateFlow(0)
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Don't expose 'state' as MutableStateFlow"
    }

    @Test
    fun `Given a public property typed as Channel When linting Then it is reported`() {
        val code = """
            class SettingsViewModel {
                val events: Channel<SettingsEvent> = Channel(Channel.BUFFERED)
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Don't expose 'events' as Channel"
    }

    @Test
    fun `Given an explicit backing field and a private channel When linting Then nothing is reported`() {
        val code = """
            class SettingsViewModel {
                val state: StateFlow<Int>
                    field = MutableStateFlow(0)

                private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
                val events = eventChannel.receiveAsFlow()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given an override and a local mutable flow When linting Then nothing is reported`() {
        val code = """
            class PersistenceRepositoryFake : PersistenceRepository {
                override val values: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())

                fun collectOnce() {
                    val buffer = Channel<Int>()
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
