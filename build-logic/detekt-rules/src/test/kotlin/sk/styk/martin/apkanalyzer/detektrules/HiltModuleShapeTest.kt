package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class HiltModuleShapeTest {

    private val rule = HiltModuleShape(Config.empty)

    @Test
    fun `Given an interface binding module and a class providing module When linting Then nothing is reported`() {
        val code = """
            @Module
            @InstallIn(SingletonComponent::class)
            internal interface AppsBindingModule {
                @Binds
                @Singleton
                fun bindAppDetailRepository(impl: AppDetailRepositoryImpl): AppDetailRepository
            }

            @Module
            @InstallIn(SingletonComponent::class)
            internal class PlatformModule {
                @Provides
                fun providePackageManager(@ApplicationContext context: Context): PackageManager = context.packageManager
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given an abstract class binding module When linting Then nothing is reported`() {
        val code = """
            @Module
            @InstallIn(SingletonComponent::class)
            internal abstract class AppsBindingModule {
                @Binds
                abstract fun bindAppDetailRepository(impl: AppDetailRepositoryImpl): AppDetailRepository
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a module without InstallIn When linting Then it is reported`() {
        val code = """
            @Module
            internal interface AppsBindingModule
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Install 'AppsBindingModule' with @InstallIn"
    }

    @Test
    fun `Given an object module When linting Then it is reported`() {
        val code = """
            @Module
            @InstallIn(SingletonComponent::class)
            object PlatformModule {
                @Provides
                fun provideClock(): Clock = Clock.systemUTC()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "not an object"
    }

    @Test
    fun `Given Binds in a concrete class module When linting Then it is reported`() {
        val code = """
            @Module
            @InstallIn(SingletonComponent::class)
            internal class AppsModule {
                @Binds
                fun bindAppDetailRepository(impl: AppDetailRepositoryImpl): AppDetailRepository = impl
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Put @Binds functions in an interface module"
    }

    @Test
    fun `Given Provides in an interface module When linting Then it is reported`() {
        val code = """
            @Module
            @InstallIn(SingletonComponent::class)
            internal interface AppsModule {
                @Provides
                fun provideClock(): Clock = Clock.systemUTC()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Put @Provides functions in a class module"
    }
}
