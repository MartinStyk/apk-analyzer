package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtAnnotationEntry

class NoKtlintSuppression(config: Config) :
    Rule(
        config,
        "ktlint findings are fixed with spotlessApply, never suppressed with @Suppress.",
    ) {

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)
        if (annotationEntry.shortName?.asString() != "Suppress") return
        val suppressesKtlint = annotationEntry.valueArguments.any { argument ->
            argument.getArgumentExpression()?.text.orEmpty().contains("ktlint")
        }
        if (suppressesKtlint) report(annotationEntry, "Don't suppress ktlint. Run ./gradlew spotlessApply and fix what remains (AGENTS.md > Guardrails).")
    }
}
