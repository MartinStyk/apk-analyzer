package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration

class NavKeyShape(config: Config) :
    Rule(
        config,
        "NavKeys are @Serializable data classes or data objects.",
    ) {

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        if ("NavKey" !in classOrObject.superTypeNames() || classOrObject.isPrivateDeclaration()) return
        if (classOrObject is KtClass && (classOrObject.isInterface() || classOrObject.isSealed())) return
        val target = classOrObject.nameIdentifier ?: classOrObject
        if (!classOrObject.hasAnnotation("Serializable")) {
            report(target, "Annotate NavKey '${classOrObject.name.orEmpty()}' with @Serializable (AGENTS.md > Architecture > Navigation).")
        }
        val isDataShape = when (classOrObject) {
            is KtObjectDeclaration -> classOrObject.isData()
            is KtClass -> classOrObject.isData()
            else -> true
        }
        if (!isDataShape) {
            report(
                target,
                "NavKey '${classOrObject.name.orEmpty()}' is a data class when it carries parameters and a data object when it " +
                    "doesn't (AGENTS.md > Architecture > Navigation).",
            )
        }
    }
}
