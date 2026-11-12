package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class BlockingOrConsoleCall(config: Config) :
    Rule(
        config,
        "Production code uses coroutines and Logger, never threads, runBlocking or console output.",
    ) {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName() ?: return
        val receiver = (expression.parent as? KtDotQualifiedExpression)
            ?.takeIf { it.selectorExpression == expression }
            ?.receiverExpression as? KtNameReferenceExpression
        val message = when {
            receiver == null && name == "runBlocking" -> "Never use runBlocking; suspend or launch in a scope (AGENTS.md > Non-Negotiables)."
            receiver == null && name == "thread" -> "Never start a thread; use coroutines (AGENTS.md > Non-Negotiables)."
            receiver == null && name == "Thread" -> "Never create a Thread; use coroutines (AGENTS.md > Non-Negotiables)."
            receiver?.getReferencedName() == "Thread" && name == "sleep" -> "Never call Thread.sleep; use delay (AGENTS.md > Non-Negotiables)."
            receiver == null && (name == "println" || name == "print") -> "Log with Logger from core.common.logger, never the console."
            name == "printStackTrace" && expression.valueArguments.isEmpty() -> "Log with Logger.e(TAG, throwable, message), never printStackTrace."
            else -> null
        }
        if (message != null) report(expression, message)
    }
}
