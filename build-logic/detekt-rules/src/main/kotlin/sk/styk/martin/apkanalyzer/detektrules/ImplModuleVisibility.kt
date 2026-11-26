package sk.styk.martin.apkanalyzer.detektrules

import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration

class ImplModuleVisibility(config: Config) :
    Rule(
        config,
        "A core implementation module exposes nothing; every top-level declaration is private or internal.",
    ) {

    override fun visit(root: KtFile) {
        super.visit(root)
        root.declarations.filterNot { it.isHiddenFromOtherModules() }.forEach { declaration ->
            report(
                declaration.nameIdentifierOrSelf(),
                "Declare '${declaration.name.orEmpty()}' internal or private. Only the app shells depend on an " +
                    "implementation module; anything a consumer needs belongs in the core api module (AGENTS.md > Module Rules).",
            )
        }
    }
}

private fun KtDeclaration.isHiddenFromOtherModules(): Boolean = hasModifier(KtTokens.PRIVATE_KEYWORD) || hasModifier(KtTokens.INTERNAL_KEYWORD)

private fun KtDeclaration.nameIdentifierOrSelf(): PsiElement = (this as? KtNamedDeclaration)?.nameIdentifier ?: this
