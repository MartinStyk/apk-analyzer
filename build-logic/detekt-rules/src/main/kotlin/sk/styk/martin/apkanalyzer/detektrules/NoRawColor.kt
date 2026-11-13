package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class NoRawColor(config: Config) :
    Rule(
        config,
        "Colors come from AppTheme.colors; only the design system defines raw colors.",
    ) {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.calleeName() != "Color") return
        val arguments = expression.valueArguments.mapNotNull { it.getArgumentExpression() }
        if (arguments.isNotEmpty() && arguments.all { it is KtConstantExpression }) {
            report(expression, MESSAGE)
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val receiver = expression.receiverExpression as? KtNameReferenceExpression ?: return
        val selector = expression.selectorExpression as? KtNameReferenceExpression ?: return
        if (receiver.getReferencedName() == "Color" && selector.getReferencedName() in NAMED_COLORS) {
            report(expression, MESSAGE)
        }
    }

    private companion object {
        const val MESSAGE = "Use a color from AppTheme.colors instead of a raw color (AGENTS.md > Compose). " +
            "Add a token to the design system if none fits."
        val NAMED_COLORS = setOf(
            "Black",
            "White",
            "Red",
            "Green",
            "Blue",
            "Yellow",
            "Cyan",
            "Magenta",
            "Gray",
            "DarkGray",
            "LightGray",
        )
    }
}
