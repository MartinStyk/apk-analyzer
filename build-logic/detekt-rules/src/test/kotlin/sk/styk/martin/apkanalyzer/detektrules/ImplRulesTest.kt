package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class ImplRulesTest {

    private val implVisibility = ImplVisibility(Config.empty)
    private val implSeparateFile = ImplSeparateFile(Config.empty)

    @Test
    fun `Given a public Impl class When linting visibility Then it is reported`() {
        val code = """
            class AppDetailRepositoryImpl @Inject constructor() : AppDetailRepository
        """.trimIndent()

        val findings = implVisibility.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Declare 'AppDetailRepositoryImpl' internal"
    }

    @Test
    fun `Given internal and private Impl classes When linting visibility Then nothing is reported`() {
        val code = """
            internal class AppDetailRepositoryImpl @Inject constructor() : AppDetailRepository

            private class CacheImpl : Cache
        """.trimIndent()

        val findings = implVisibility.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given an interface and its Impl in one file When linting file layout Then the Impl is reported`() {
        val code = """
            interface AppDetailRepository {
                suspend fun getAppDetail(packageName: String): Result<AppDetail>
            }

            internal class AppDetailRepositoryImpl @Inject constructor() : AppDetailRepository {
                override suspend fun getAppDetail(packageName: String) = Result.failure<AppDetail>(IllegalStateException())
            }
        """.trimIndent()

        val findings = implSeparateFile.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Move 'AppDetailRepositoryImpl' to its own file"
    }

    @Test
    fun `Given an Impl whose interface lives in another file When linting file layout Then nothing is reported`() {
        val code = """
            internal class AppDetailRepositoryImpl @Inject constructor() : AppDetailRepository
        """.trimIndent()

        val findings = implSeparateFile.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given an interface with its models in one file When linting file layout Then nothing is reported`() {
        val code = """
            interface AppDetailRepository {
                suspend fun getAppDetail(packageName: String): Result<AppDetail>
            }

            data class AppDetail(val packageName: String)
        """.trimIndent()

        val findings = implSeparateFile.lint(code)

        findings.shouldBeEmpty()
    }
}
