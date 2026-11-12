package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class SealedMemberDataObject(config: Config) :
    Rule(
        config,
        "Objects that implement a sealed type are data objects.",
    ) {

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        super.visitObjectDeclaration(declaration)
        if (declaration.isCompanion() || declaration.isObjectLiteral() || declaration.isData()) return
        if (declaration.implementsSealedType()) {
            report(
                declaration.nameIdentifier ?: declaration,
                "Declare '${declaration.name.orEmpty()}' as a data object; it is a member of a sealed type " +
                    "(AGENTS.md > Conventions).",
            )
        }
    }

    private fun KtObjectDeclaration.implementsSealedType(): Boolean {
        val superTypes = superTypeNames().toSet()
        if (superTypes.isEmpty()) return false
        val enclosingSealed = generateSequence(getStrictParentOfType<KtClass>()) { it.getStrictParentOfType<KtClass>() }
            .any { it.isSealed() && it.name in superTypes }
        if (enclosingSealed) return true
        return containingKtFile.declarations
            .filterIsInstance<KtClass>()
            .flatMap { listOf(it) + it.nestedClasses() }
            .any { it.isSealed() && it.name in superTypes }
    }

    private fun KtClass.nestedClasses(): List<KtClass> = body?.declarations.orEmpty().filterIsInstance<KtClass>().flatMap { listOf(it) + it.nestedClasses() }
}
