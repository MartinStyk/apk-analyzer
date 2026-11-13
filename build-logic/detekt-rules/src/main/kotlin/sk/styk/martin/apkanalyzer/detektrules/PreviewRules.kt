package sk.styk.martin.apkanalyzer.detektrules

import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class PreviewShape(config: Config) :
    Rule(
        config,
        "Previews are suffixed Preview and wrapped in the app theme.",
    ) {

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isComposable() || !function.isPreview()) return
        val target = function.nameIdentifier ?: function
        if (function.name?.endsWith("Preview") != true) {
            report(target, "Suffix preview '${function.name.orEmpty()}' with Preview (AGENTS.md > Compose).")
        }
        val usesTheme = PsiTreeUtil.collectElementsOfType(function, KtCallExpression::class.java)
            .any { it.calleeName().isAppTheme() }
        if (!usesTheme) {
            report(target, "Wrap preview '${function.name.orEmpty()}' in ApkAnalyzerTheme { } or ApkAnalyzerTvTheme { } (AGENTS.md > Compose).")
        }
    }

    private fun String?.isAppTheme(): Boolean = this != null && startsWith("ApkAnalyzer") && "Theme" in this
}

class PreviewPresence(config: Config) :
    Rule(
        config,
        "Every file that declares a non-private composable also declares a @Preview.",
    ) {

    override fun visit(root: KtFile) {
        super.visit(root)
        val functions = root.declarations.filterIsInstance<KtNamedFunction>()
        val publicComposables = functions.filter { function ->
            function.isComposable() && !function.isPreview() && !function.isPrivateDeclaration() &&
                function.receiverTypeReference == null && function.name?.firstOrNull()?.isUpperCase() == true
        }
        if (publicComposables.isEmpty() || functions.any { it.isPreview() }) return
        val first = publicComposables.first()
        report(
            first.nameIdentifier ?: first,
            "Add a private @Preview for the stateless composables in this file, wrapped in the app theme with " +
                "realistic sample data (AGENTS.md > Compose).",
        )
    }
}
