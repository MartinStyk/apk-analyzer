package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.parents

class HardcodedComposeString(config: Config) :
    Rule(
        config,
        "User-facing text in composables comes from stringResource, never a literal.",
    ) {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee = expression.calleeName() ?: return
        if (!callee.first().isUpperCase() || expression.isInsidePreviewCode()) return
        expression.valueArguments
            .filter { argument -> argument.isUserFacingText(callee, expression) }
            .forEach { argument ->
                report(
                    argument,
                    "Move this user-facing text to res/values/strings.xml and read it with stringResource " +
                        "(AGENTS.md > Non-Negotiables).",
                )
            }
    }

    private fun KtValueArgument.isUserFacingText(callee: String, call: KtCallExpression): Boolean {
        val template = getArgumentExpression() as? KtStringTemplateExpression ?: return false
        val hasLetters = template.entries.filterIsInstance<KtLiteralStringTemplateEntry>().any { entry -> entry.text.any(Char::isLetter) }
        if (!hasLetters) return false
        val argumentName = getArgumentName()?.asName?.asString()
        return when {
            argumentName == "label" && callee in ANIMATION_CALLEES -> false
            argumentName != null -> argumentName in TEXT_PARAMETERS
            else -> callee.endsWith("Text") && call.valueArguments.firstOrNull() == this
        }
    }

    private fun KtCallExpression.isInsidePreviewCode(): Boolean = parents.filterIsInstance<KtNamedDeclaration>().any { declaration ->
        val name = declaration.name.orEmpty()
        (declaration as? KtNamedFunction)?.isPreview() == true ||
            name.endsWith("Preview") ||
            name.startsWith("sample") ||
            name.startsWith("preview")
    }

    private companion object {
        val ANIMATION_CALLEES = setOf("Crossfade", "AnimatedContent", "AnimatedVisibility")
        val TEXT_PARAMETERS = setOf("text", "contentDescription", "label", "title", "placeholder", "subtitle", "description")
    }
}
