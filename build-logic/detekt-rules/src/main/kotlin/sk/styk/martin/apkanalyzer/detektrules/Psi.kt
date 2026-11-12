package sk.styk.martin.apkanalyzer.detektrules

import com.intellij.psi.PsiElement
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

internal fun Rule.report(element: PsiElement, message: String) {
    report(Finding(Entity.from(element), message))
}

internal fun KtAnnotated.hasAnnotation(vararg names: String): Boolean = annotationEntries.any { it.shortName?.asString() in names }

internal fun KtModifierListOwner.isPrivateDeclaration(): Boolean = hasModifier(KtTokens.PRIVATE_KEYWORD)

internal fun KtModifierListOwner.isHiddenFromOtherModules(): Boolean = hasModifier(KtTokens.PRIVATE_KEYWORD) || hasModifier(KtTokens.INTERNAL_KEYWORD)

internal fun KtModifierListOwner.isOverride(): Boolean = hasModifier(KtTokens.OVERRIDE_KEYWORD)

internal fun KtTypeReference.baseTypeName(): String = text.baseTypeName()

internal fun String.baseTypeName(): String = substringBefore('<').removeSuffix("?").substringAfterLast('.').trim()

internal fun KtClassOrObject.superTypeNames(): List<String> = superTypeListEntries.mapNotNull { entry -> entry.typeReference?.baseTypeName() }

internal fun KtCallExpression.calleeName(): String? = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()

internal fun PsiElement.enclosingFunction(): KtNamedFunction? = getStrictParentOfType<KtNamedFunction>()

internal fun KtNamedFunction.isComposable(): Boolean = hasAnnotation("Composable")

internal fun KtNamedFunction.isPreview(): Boolean = annotationEntries.any { entry ->
    val name = entry.shortName?.asString().orEmpty()
    name.startsWith("Preview") || name.endsWith("Preview") || name.endsWith("Previews")
}
