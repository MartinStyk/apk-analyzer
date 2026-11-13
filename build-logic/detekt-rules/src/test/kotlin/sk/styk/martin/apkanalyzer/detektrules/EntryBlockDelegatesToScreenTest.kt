package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class EntryBlockDelegatesToScreenTest {

    private val rule = EntryBlockDelegatesToScreen(Config.empty)

    @Test
    fun `Given an entry calling one screen When linting Then nothing is reported`() {
        val code = """
            fun EntryProviderScope<NavKey>.appDetailEntries(navigator: Navigator) {
                entry<AppDetailKey>(metadata = slideFromEndEntryMetadata()) { key ->
                    AppDetailScreen(
                        packageName = key.packageName,
                        onBack = { navigator.goBack() },
                    )
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given an entry with two statements When linting Then it is reported`() {
        val code = """
            fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
                entry<SettingsKey> {
                    val title = "Settings"
                    SettingsScreen(onBack = { navigator.goBack() })
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "calls exactly one *Screen"
    }

    @Test
    fun `Given an entry wrapping the screen in a layout When linting Then it is reported`() {
        val code = """
            fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
                entry<SettingsKey> {
                    Box {
                        SettingsScreen(onBack = { navigator.goBack() })
                    }
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
    }

    @Test
    fun `Given an entry reading remember and a composition local When linting Then both are reported`() {
        val code = """
            fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
                entry<SettingsKey> {
                    SettingsScreen(
                        context = LocalContext.current,
                        scrollState = rememberScrollState(),
                    )
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 2
        findings.first().message shouldContain "never uses remember or composition locals"
    }

    @Test
    fun `Given an entry call without a key type When linting Then nothing is reported`() {
        val code = """
            fun register() {
                entry {
                    Box()
                    Text()
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
