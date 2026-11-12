package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtNamedFunction

class PresentTenseCallbacks(config: Config) :
    Rule(
        config,
        "Composable callbacks are present tense: onClick, onSelectItem, onBack.",
    ) {

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isComposable()) return
        function.valueParameters
            .filter { parameter -> parameter.name?.matches(PAST_TENSE_CALLBACK) == true }
            .forEach { parameter ->
                report(
                    parameter,
                    "Name the callback '${parameter.name.orEmpty()}' in the present tense, such as onClick or onSelectItem " +
                        "(AGENTS.md > Compose).",
                )
            }
    }

    private companion object {
        val PAST_TENSE_CALLBACK = Regex("^on(?=[A-Z])\\w*(Clicked|Pressed|Selected|Changed|Tapped|Dismissed|Toggled|Checked)$")
    }
}
