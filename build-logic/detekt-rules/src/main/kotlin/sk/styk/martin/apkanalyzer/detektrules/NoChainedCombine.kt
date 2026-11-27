package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression

class NoChainedCombine(config: Config) :
    Rule(
        config,
        "Combine all input flows in one call instead of chaining combine calls.",
    ) {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (!expression.isFlowCombine()) return
        val qualified = expression.parent as? KtQualifiedExpression ?: return
        if (qualified.selectorExpression != expression || !qualified.receiverExpression.containsCombine()) return
        report(
            expression,
            "Pass all input flows to one combine(...) call instead of chaining combine calls. " +
                "Use the typed core.common.coroutines.combine overload for six inputs (AGENTS.md > Conventions).",
        )
    }

    private fun KtExpression.containsCombine(): Boolean = when (this) {
        is KtParenthesizedExpression -> expression?.containsCombine() == true

        is KtCallExpression -> isFlowCombine()

        is KtQualifiedExpression ->
            (selectorExpression as? KtCallExpression)?.isFlowCombine() == true || receiverExpression.containsCombine()

        else -> false
    }

    private fun KtCallExpression.isFlowCombine(): Boolean {
        val name = calleeName() ?: return false
        val qualified = parent as? KtQualifiedExpression
        if (qualified?.selectorExpression == this && "${qualified.receiverExpression.text}.$name" in COMBINE_FUNCTIONS) return true
        return containingKtFile.importDirectives.any { directive ->
            directive.importedFqName?.asString() in COMBINE_FUNCTIONS &&
                name == (directive.aliasName ?: "combine")
        }
    }

    private companion object {
        val COMBINE_FUNCTIONS = setOf(
            "kotlinx.coroutines.flow.combine",
            "sk.styk.martin.apkanalyzer.core.common.coroutines.combine",
        )
    }
}
