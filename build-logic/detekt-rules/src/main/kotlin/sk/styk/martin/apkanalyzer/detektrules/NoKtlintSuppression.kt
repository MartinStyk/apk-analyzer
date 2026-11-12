package sk.styk.martin.apkanalyzer.detektrules

import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile

class NoKtlintSuppression(config: Config) :
    Rule(
        config,
        "Formatting rules are fixed with spotlessApply, never suppressed.",
    ) {

    override fun visit(root: KtFile) {
        super.visit(root)
        PsiTreeUtil.collectElementsOfType(root, PsiComment::class.java)
            .filter { it.text.contains("ktlint-disable") }
            .forEach { report(it, SUPPRESSION_MESSAGE) }
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)
        val name = annotationEntry.shortName?.asString()
        if (name != "Suppress" && name != "SuppressWarnings") return
        val suppressesKtlint = annotationEntry.valueArguments.any { argument ->
            argument.getArgumentExpression()?.text.orEmpty().contains("ktlint")
        }
        if (suppressesKtlint) report(annotationEntry, SUPPRESSION_MESSAGE)
    }

    private companion object {
        const val SUPPRESSION_MESSAGE = "Don't suppress ktlint. Run ./gradlew spotlessApply and fix what remains."
    }
}
