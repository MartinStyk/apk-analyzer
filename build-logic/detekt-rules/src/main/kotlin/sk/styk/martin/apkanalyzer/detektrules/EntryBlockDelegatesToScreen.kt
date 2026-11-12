package sk.styk.martin.apkanalyzer.detektrules

import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class EntryBlockDelegatesToScreen(config: Config) :
    Rule(
        config,
        "A Navigation 3 entry { } block only delegates to one *Screen composable.",
    ) {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.calleeName() != "entry" || expression.typeArguments.isEmpty()) return
        val lambda = expression.lambdaArguments.singleOrNull()?.getLambdaExpression() ?: return
        val statements = lambda.bodyExpression?.statements.orEmpty()
        val screenCall = statements.singleOrNull() as? KtCallExpression
        if (screenCall == null || screenCall.calleeName()?.endsWith("Screen") != true) {
            report(
                lambda,
                "An entry { } block calls exactly one *Screen and nothing else. Move layout, state and platform " +
                    "calls into the screen (AGENTS.md > Architecture > Navigation).",
            )
            return
        }
        reportComposedState(lambda)
    }

    private fun reportComposedState(lambda: KtLambdaExpression) {
        PsiTreeUtil.collectElementsOfType(lambda, KtNameReferenceExpression::class.java)
            .filter { reference ->
                val name = reference.getReferencedName()
                name.startsWith("remember") || name.matches(LOCAL_COMPOSITION_REGEX)
            }
            .forEach { reference ->
                report(
                    reference,
                    "An entry { } block never uses remember or composition locals. Read them inside the screen " +
                        "(AGENTS.md > Architecture > Navigation).",
                )
            }
    }

    private companion object {
        val LOCAL_COMPOSITION_REGEX = Regex("^Local[A-Z]\\w*")
    }
}
