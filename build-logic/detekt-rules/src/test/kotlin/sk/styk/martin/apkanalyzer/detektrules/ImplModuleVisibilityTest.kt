package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class ImplModuleVisibilityTest {

    private val rule = ImplModuleVisibility(Config.empty)

    @Test
    fun `Given public top-level declarations When linting Then each is reported`() {
        val code = """
            class PlayEntitlementsRepository : EntitlementsRepository

            object BillingConstants

            fun Purchase.isAcknowledgedPurchase(): Boolean = isAcknowledged

            val defaultProducts = listOf("pro")

            typealias ProductId = String
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 5
        findings.first().message shouldContain "Declare 'PlayEntitlementsRepository' internal or private"
    }

    @Test
    fun `Given an explicitly public declaration When linting Then it is reported`() {
        val code = """
            public interface BillingClientFactory
        """.trimIndent()

        val findings = rule.lint(code)

        findings.single().message shouldContain "Declare 'BillingClientFactory' internal or private"
    }

    @Test
    fun `Given internal and private top-level declarations When linting Then nothing is reported`() {
        val code = """
            internal class PlayEntitlementsRepository : EntitlementsRepository {
                override val entitlements: Flow<Set<Entitlement>> = emptyFlow()
                fun refresh() = Unit
            }

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface EntitlementsModule

            private const val TAG = "Billing"

            internal fun Purchase.isAcknowledgedPurchase(): Boolean = isAcknowledged
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a public member of an internal class When linting Then only the class visibility counts`() {
        val code = """
            internal class BillingConnection {
                class Listener
                val isReady: Boolean = false
            }
        """.trimIndent()

        rule.lint(code).shouldBeEmpty()
    }
}
