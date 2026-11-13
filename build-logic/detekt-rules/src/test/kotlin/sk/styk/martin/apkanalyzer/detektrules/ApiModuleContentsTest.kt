package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class ApiModuleContentsTest {

    private val rule = ApiModuleContents(Config.empty)

    @Test
    fun `Given serializable keys their carried types and a serializer When linting Then nothing is reported`() {
        val code = """
            @Serializable
            data class AppDetailKey(val packageName: String, val source: Source) : NavKey

            @Serializable
            enum class Source { Installed, File }

            object UriSerializer : KSerializer<Uri> {
                override val descriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a top level function in an api module When linting Then it is reported`() {
        val code = """
            fun appDetailKey(packageName: String) = AppDetailKey(packageName)
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Move 'appDetailKey' to the feature's domain or phone module"
    }

    @Test
    fun `Given a non serializable class in an api module When linting Then it is reported`() {
        val code = """
            class AppDetailNavigator(private val navigator: Navigator)
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Move 'AppDetailNavigator'"
    }
}
