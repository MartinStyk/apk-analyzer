package sk.styk.martin.apkanalyzer.detektrules

import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

class NoComments(config: Config) :
    Rule(
        config,
        "Code must not contain comments or KDoc, except KDoc that App Functions read as the tool description.",
    ) {

    override fun visit(root: KtFile) {
        super.visit(root)
        PsiTreeUtil.collectElementsOfType(root, PsiComment::class.java)
            .filterNot { comment -> comment is KDoc && comment.describesAppFunction() }
            .forEach { comment ->
                report(
                    comment,
                    "Comments and KDoc are not allowed (AGENTS.md > Non-Negotiables). Use a self-documenting name or " +
                        "extract a well-named function instead.",
                )
            }
    }

    private fun KDoc.describesAppFunction(): Boolean = getOwner()?.parentsWithSelf.orEmpty()
        .filterIsInstance<KtAnnotated>()
        .any { declaration ->
            declaration.annotationEntries.any { it.shortName?.asString()?.startsWith("AppFunction") == true }
        }
}
