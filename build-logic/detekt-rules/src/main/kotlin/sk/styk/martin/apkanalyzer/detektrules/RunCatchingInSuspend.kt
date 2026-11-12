package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtValueArgument

class RunCatchingInSuspend(config: Config) :
    Rule(
        config,
        "runCatching swallows CancellationException inside coroutines; use runCatchingCancellable.",
    ) {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.calleeName() != "runCatching" || !expression.isInCoroutine()) return
        report(
            expression,
            "runCatching swallows CancellationException in a coroutine. Use runCatchingCancellable from " +
                "core.common.coroutines instead.",
        )
    }

    private fun KtCallExpression.isInCoroutine(): Boolean {
        var current = parent
        while (current != null) {
            when (current) {
                is KtNamedFunction -> return current.hasModifier(KtTokens.SUSPEND_KEYWORD)
                is KtLambdaExpression -> if (current.isCoroutineBuilderLambda()) return true
            }
            current = current.parent
        }
        return false
    }

    private fun KtLambdaExpression.isCoroutineBuilderLambda(): Boolean {
        val argument = parent as? KtValueArgument ?: (parent as? KtLambdaArgument)
        val call = argument?.parent?.let { it as? KtCallExpression ?: it.parent as? KtCallExpression }
        return call?.calleeName() in COROUTINE_BUILDERS
    }

    private companion object {
        val COROUTINE_BUILDERS = setOf(
            "launch",
            "async",
            "withContext",
            "flow",
            "channelFlow",
            "callbackFlow",
            "coroutineScope",
            "supervisorScope",
            "transform",
            "mapLatest",
            "flatMapLatest",
            "collect",
            "collectLatest",
            "onEach",
            "LaunchedEffect",
            "produceState",
            "runTest",
        )
    }
}
