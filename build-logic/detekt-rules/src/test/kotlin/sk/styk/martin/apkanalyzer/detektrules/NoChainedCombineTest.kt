package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class NoChainedCombineTest {

    private val rule = NoChainedCombine(Config.empty)

    @Test
    fun `Given chained combine calls When linting Then the second call is reported`() {
        val code = """
            import kotlinx.coroutines.flow.combine

            val selection = combine(step, apps, format) { step, apps, format ->
                InventoryExportState(step, apps, format)
            }.combine(proOwnership) { selection, ownership -> selection.copy(proOwnership = ownership) }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "one combine(...) call"
    }

    @Test
    fun `Given parenthesized combine with intervening operators When linting Then the chain is reported`() {
        val code = """
            import kotlinx.coroutines.flow.combine

            val state = (first.combine(second) { a, b -> a + b })
                .map { it * 2 }
                .combine(third) { value, c -> value + c }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given an aliased six input combine When linting Then a chained call is reported`() {
        val code = """
            import sk.styk.martin.apkanalyzer.core.common.coroutines.combine as combineSix
            import kotlinx.coroutines.flow.combine as combineFlow

            val state = combineSix(a, b, c, d, e, f) { a, b, c, d, e, f -> State(a, b, c, d, e, f) }
                .combineFlow(g) { state, g -> state.copy(g = g) }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given a fully qualified combine When linting Then a chained call is reported`() {
        val code = """
            import kotlinx.coroutines.flow.combine

            val state = kotlinx.coroutines.flow.combine(a, b) { a, b -> a + b }
                .combine(c) { value, c -> value + c }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given three chained combine calls When linting Then both extra calls are reported`() {
        val code = """
            import kotlinx.coroutines.flow.combine

            val state = combine(a, b) { a, b -> a + b }
                .combine(c) { value, c -> value + c }
                .combine(d) { value, d -> value + d }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 2
    }

    @Test
    fun `Given single combine calls When linting Then nothing is reported`() {
        val code = """
            import kotlinx.coroutines.flow.combine
            import sk.styk.martin.apkanalyzer.core.common.coroutines.combine

            val state = combine(a, b, c, d, e, f) { a, b, c, d, e, f -> State(a, b, c, d, e, f) }
                .stateIn(scope, started, initial)
            val pair = a.combine(b) { a, b -> a + b }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given an independent combine inside a transform When linting Then nothing is reported`() {
        val code = """
            import kotlinx.coroutines.flow.combine

            val state = a.map { combine(b, c) { b, c -> b + c } }
                .combine(d) { innerFlow, d -> State(innerFlow, d) }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given unrelated combine functions When linting Then nothing is reported`() {
        val code = """
            import example.combine

            val text = combine(a, b).combine(c)
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
