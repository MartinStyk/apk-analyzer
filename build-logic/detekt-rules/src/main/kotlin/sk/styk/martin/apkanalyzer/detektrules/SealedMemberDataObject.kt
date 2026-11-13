package sk.styk.martin.apkanalyzer.detektrules

import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration

class SealedMemberDataObject(config: Config) :
    Rule(
        config,
        "Objects that implement a sealed type are data objects.",
    ) {

    override fun visit(root: KtFile) {
        super.visit(root)
        val sealedTypes = PsiTreeUtil.collectElementsOfType(root, KtClass::class.java)
            .filter { it.isSealed() }
            .mapNotNull { it.name }
            .toSet()
        if (sealedTypes.isEmpty()) return
        PsiTreeUtil.collectElementsOfType(root, KtObjectDeclaration::class.java)
            .filter { !it.isCompanion() && !it.hasModifier(KtTokens.DATA_KEYWORD) && it.superTypeNames().any(sealedTypes::contains) }
            .forEach { member ->
                report(
                    member.nameIdentifier ?: member,
                    "Declare '${member.name.orEmpty()}' as a data object (AGENTS.md > Conventions).",
                )
            }
    }
}
