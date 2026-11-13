package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject

class ImplVisibility(config: Config) :
    Rule(
        config,
        "An *Impl class is internal; callers depend on its interface.",
    ) {

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        if (classOrObject.name?.endsWith("Impl") != true || classOrObject.isHiddenFromOtherModules()) return
        report(
            classOrObject.nameIdentifier ?: classOrObject,
            "Declare '${classOrObject.name.orEmpty()}' internal and expose its interface (AGENTS.md > Architecture > Data Layer).",
        )
    }
}

private fun KtClassOrObject.isHiddenFromOtherModules(): Boolean = hasModifier(KtTokens.PRIVATE_KEYWORD) || hasModifier(KtTokens.INTERNAL_KEYWORD)
