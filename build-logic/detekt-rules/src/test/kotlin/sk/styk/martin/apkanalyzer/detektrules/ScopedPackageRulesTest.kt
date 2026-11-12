package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class ScopedPackageRulesTest {

    private val material3Scope = Material3Scope(Config.empty)
    private val tvMaterial3Scope = TvMaterial3Scope(Config.empty)
    private val materialIconsScope = MaterialIconsScope(Config.empty)

    @Test
    fun `Given a Material3 import When linting Then it is reported once`() {
        val code = """
            import androidx.compose.material3.Text
        """.trimIndent()

        val findings = material3Scope.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "androidx.compose.material3 is not allowed here"
    }

    @Test
    fun `Given a fully qualified Material3 expression When linting Then it is reported once`() {
        val code = """
            fun primaryColor() = androidx.compose.material3.MaterialTheme.colorScheme.primary
        """.trimIndent()

        val findings = material3Scope.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given a fully qualified Material3 type When linting Then it is reported once`() {
        val code = """
            fun scheme(colors: androidx.compose.material3.ColorScheme) = colors
        """.trimIndent()

        val findings = material3Scope.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given foundation and design system imports When linting Then nothing is reported`() {
        val code = """
            import androidx.compose.foundation.text.BasicText
            import sk.styk.martin.apkanalyzer.core.designsystem.components.ApkAnalyzerText
        """.trimIndent()

        val findings = material3Scope.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a TV Material3 import When linting Then it is reported`() {
        val code = """
            import androidx.tv.material3.Surface
        """.trimIndent()

        val findings = tvMaterial3Scope.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "androidx.tv.material3 is not allowed here"
    }

    @Test
    fun `Given a phone Material3 import When linting with the TV rule Then nothing is reported`() {
        val code = """
            import androidx.compose.material3.Text
        """.trimIndent()

        val findings = tvMaterial3Scope.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a Material icons import When linting Then it is reported`() {
        val code = """
            import androidx.compose.material.icons.Icons
        """.trimIndent()

        val findings = materialIconsScope.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Use ApkAnalyzerIcons"
    }

    @Test
    fun `Given an ApkAnalyzerIcons usage When linting with the icons rule Then nothing is reported`() {
        val code = """
            import sk.styk.martin.apkanalyzer.core.designsystem.icons.ApkAnalyzerIcons

            val searchIcon = ApkAnalyzerIcons.Search
        """.trimIndent()

        val findings = materialIconsScope.lint(code)

        findings.shouldBeEmpty()
    }
}
