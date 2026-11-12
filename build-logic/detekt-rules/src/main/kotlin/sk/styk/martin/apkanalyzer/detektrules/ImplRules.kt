package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

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

class ImplSeparateFile(config: Config) :
    Rule(
        config,
        "An interface and its *Impl live in separate files.",
    ) {

    override fun visit(root: KtFile) {
        super.visit(root)
        val classes = root.declarations.filterIsInstance<KtClass>()
        val interfaceNames = classes.filter { it.isInterface() }.mapNotNull { it.name }.toSet()
        classes
            .filter { it.name?.endsWith("Impl") == true && it.superTypeNames().any { superType -> superType in interfaceNames } }
            .forEach { impl ->
                report(
                    impl.nameIdentifier ?: impl,
                    "Move '${impl.name.orEmpty()}' to its own file; keep the interface and its models in the interface file " +
                        "(AGENTS.md > Architecture > Data Layer).",
                )
            }
    }
}
