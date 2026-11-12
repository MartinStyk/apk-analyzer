package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class BlockingOrConsoleCallTest {

    private val rule = BlockingOrConsoleCall(Config.empty)

    @Test
    fun `Given runBlocking When linting Then it is reported`() {
        val code = """
            fun loadApps() = runBlocking { repository.loadApps() }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Never use runBlocking"
    }

    @Test
    fun `Given a started Thread When linting Then it is reported`() {
        val code = """
            fun export() {
                Thread { writer.write() }.start()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Never create a Thread"
    }

    @Test
    fun `Given the thread builder When linting Then it is reported`() {
        val code = """
            fun export() {
                thread { writer.write() }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Never start a thread"
    }

    @Test
    fun `Given Thread sleep When linting Then it is reported`() {
        val code = """
            fun retry() {
                Thread.sleep(500)
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Never call Thread.sleep"
    }

    @Test
    fun `Given println and printStackTrace When linting Then both are reported`() {
        val code = """
            fun parse() {
                try {
                    println("parsing")
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 2
    }

    @Test
    fun `Given coroutine and Logger calls and a print on a writer When linting Then nothing is reported`() {
        val code = """
            suspend fun export(writer: PrintWriter, error: Throwable) {
                delay(500)
                scope.launch { repository.loadApps() }
                writer.print("packageName")
                writer.println("versionName")
                error.printStackTrace(writer)
                Logger.e(TAG, error, "Export failed")
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
