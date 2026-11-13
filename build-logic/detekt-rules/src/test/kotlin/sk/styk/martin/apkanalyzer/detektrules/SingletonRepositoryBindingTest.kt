package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class SingletonRepositoryBindingTest {

    private val rule = SingletonRepositoryBinding(Config.empty)

    @Test
    fun `Given an unscoped repository binding When linting Then it is reported`() {
        val code = """
            @Module
            @InstallIn(SingletonComponent::class)
            internal interface AppsModule {

                @Binds
                fun bindInstalledAppsRepository(impl: InstalledAppsRepositoryImpl): InstalledAppsRepository
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Scope the InstalledAppsRepository binding with @Singleton"
    }

    @Test
    fun `Given a scoped manager and an unscoped parser When linting Then nothing is reported`() {
        val code = """
            @Module
            @InstallIn(SingletonComponent::class)
            internal interface AppsModule {

                @Binds
                @Singleton
                fun bindAppExportManager(impl: AppExportManagerImpl): AppExportManager

                @Binds
                fun bindManifestParser(impl: ManifestParserImpl): ManifestParser
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
