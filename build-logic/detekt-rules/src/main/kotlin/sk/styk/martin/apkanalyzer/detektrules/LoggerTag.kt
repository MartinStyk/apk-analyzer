package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

class LoggerTag(config: Config) :
    Rule(
        config,
        "A log TAG is a file-level private const val.",
    ) {

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (property.name != "TAG") return
        val isTopLevel = property.parent is KtFile
        val isPrivateConst = property.isPrivateDeclaration() && property.hasModifier(KtTokens.CONST_KEYWORD)
        if (!isTopLevel || !isPrivateConst) {
            report(
                property,
                "Declare TAG as a file-level `private const val TAG` (AGENTS.md > Conventions).",
            )
        }
    }
}
