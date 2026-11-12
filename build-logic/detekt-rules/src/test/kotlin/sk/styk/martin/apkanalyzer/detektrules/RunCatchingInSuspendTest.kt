package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class RunCatchingInSuspendTest {

    private val rule = RunCatchingInSuspend(Config.empty)

    @Test
    fun `Given runCatching in a suspend function When linting Then it is reported`() {
        val code = """
            suspend fun loadApps(): Result<List<String>> = runCatching { repository.loadApps() }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Use runCatchingCancellable"
    }

    @Test
    fun `Given runCatching inside a launch block of a regular function When linting Then it is reported`() {
        val code = """
            fun refresh() {
                viewModelScope.launch {
                    val apps = runCatching { repository.loadApps() }
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given runCatching inside withContext When linting Then it is reported`() {
        val code = """
            fun parse(dispatcher: CoroutineDispatcher) = flow {
                withContext(dispatcher) {
                    runCatching { parser.parse() }
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given runCatching in a regular function and its non coroutine lambdas When linting Then nothing is reported`() {
        val code = """
            fun parseCertificates(bytes: List<ByteArray>) = bytes.map { runCatching { parse(it) } }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given runCatchingCancellable in a suspend function When linting Then nothing is reported`() {
        val code = """
            suspend fun loadApps(): Result<List<String>> = runCatchingCancellable { repository.loadApps() }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
