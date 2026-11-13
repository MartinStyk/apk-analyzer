package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class ImplVisibilityTest {

    private val rule = ImplVisibility(Config.empty)

    @Test
    fun `Given a public Impl class When linting Then it is reported`() {
        val code = """
            class AppDetailRepositoryImpl @Inject constructor() : AppDetailRepository
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Declare 'AppDetailRepositoryImpl' internal"
    }

    @Test
    fun `Given internal and private Impl classes When linting Then nothing is reported`() {
        val code = """
            internal class AppDetailRepositoryImpl @Inject constructor() : AppDetailRepository

            private class CacheImpl : Cache
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
