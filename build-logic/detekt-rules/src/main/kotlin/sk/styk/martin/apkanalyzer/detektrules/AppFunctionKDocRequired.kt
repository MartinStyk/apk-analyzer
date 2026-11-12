package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction

class AppFunctionKDocRequired(config: Config) :
    Rule(
        config,
        "App Function declarations carry KDoc, because agents read it as the tool description.",
    ) {

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (function.hasAnnotation("AppFunction")) requireKDoc(function)
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        if (classOrObject.hasAnnotation("AppFunctionSerializable")) requireKDoc(classOrObject)
    }

    private fun requireKDoc(declaration: KtDeclaration) {
        if (declaration.docComment != null) return
        report(
            (declaration as? KtNamedFunction)?.nameIdentifier ?: (declaration as? KtClassOrObject)?.nameIdentifier ?: declaration,
            "Document '${declaration.name.orEmpty()}' with KDoc; on-device agents read it as the tool description " +
                "(core/app-functions/AGENTS.md).",
        )
    }
}
