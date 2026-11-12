package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtUserType

open class ScopedPackageRule(
    config: Config,
    description: String,
    private val packagePrefix: String,
    private val guidance: String,
) : Rule(config, description) {

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        if (importDirective.importedFqName?.asString().isInScope()) {
            reportUsage(importDirective)
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val parent = expression.parent
        val isOutermost = !(parent is KtDotQualifiedExpression && parent.receiverExpression == expression)
        if (isOutermost && expression.getStrictImportParent() == null && expression.compactText().isInScope()) {
            reportUsage(expression)
        }
    }

    override fun visitUserType(type: KtUserType) {
        super.visitUserType(type)
        if (type.parent !is KtUserType && type.compactText().isInScope()) {
            reportUsage(type)
        }
    }

    private fun String?.isInScope(): Boolean = this != null && startsWith("$packagePrefix.")

    private fun KtElement.compactText(): String = text.filterNot(Char::isWhitespace)

    private fun KtDotQualifiedExpression.getStrictImportParent(): KtImportDirective? =
        generateSequence(parent) { it.parent }.filterIsInstance<KtImportDirective>().firstOrNull()

    private fun reportUsage(element: KtElement) {
        report(element, "$packagePrefix is not allowed here. $guidance")
    }
}

class Material3Scope(config: Config) :
    ScopedPackageRule(
        config = config,
        description = "Material3 is only used inside core:design-system:phone and the app module's theme plumbing.",
        packagePrefix = "androidx.compose.material3",
        guidance = "Use the wrappers in core:design-system:phone, or wrap the component there first " +
            "(AGENTS.md > Compose, create-compose-component skill).",
    )

class TvMaterial3Scope(config: Config) :
    ScopedPackageRule(
        config = config,
        description = "TV Material3 is only used inside core:design-system:tv.",
        packagePrefix = "androidx.tv.material3",
        guidance = "Use the wrappers in core:design-system:tv, or wrap the component there first (AGENTS.md > Compose).",
    )

class MaterialIconsScope(config: Config) :
    ScopedPackageRule(
        config = config,
        description = "Material icons are only referenced from ApkAnalyzerIcons in the design system.",
        packagePrefix = "androidx.compose.material.icons",
        guidance = "Use ApkAnalyzerIcons; add the icon there first (AGENTS.md > Compose).",
    )
