package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration

private const val HILT_CONTRACT = "AGENTS.md > Architecture > Hilt"

class HiltModuleShape(config: Config) :
    Rule(
        config,
        "Hilt modules are installed in a component, bind interfaces in an interface and provide only platform types in a class.",
    ) {

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        if (!classOrObject.hasAnnotation("Module")) return
        val target = classOrObject.nameIdentifier ?: classOrObject
        if (!classOrObject.hasAnnotation("InstallIn")) {
            report(target, "Install '${classOrObject.name.orEmpty()}' with @InstallIn(SingletonComponent::class) ($HILT_CONTRACT).")
        }
        if (classOrObject is KtObjectDeclaration) {
            report(target, "Declare Hilt module '${classOrObject.name.orEmpty()}' as an interface or a class, not an object ($HILT_CONTRACT).")
        }
        val functions = classOrObject.body?.declarations.orEmpty().filterIsInstance<KtNamedFunction>()
        val isAbstractShape = classOrObject is KtClass && (classOrObject.isInterface() || classOrObject.hasModifier(KtTokens.ABSTRACT_KEYWORD))
        if (!isAbstractShape) {
            functions.filter { it.hasAnnotation("Binds") }.forEach {
                report(it, "Put @Binds functions in an interface module ($HILT_CONTRACT).")
            }
        }
        if (classOrObject is KtClass && classOrObject.isInterface()) {
            functions.filter { it.hasAnnotation("Provides") }.forEach {
                report(it, "Put @Provides functions in a class module, and only for platform types ($HILT_CONTRACT).")
            }
        }
    }
}
