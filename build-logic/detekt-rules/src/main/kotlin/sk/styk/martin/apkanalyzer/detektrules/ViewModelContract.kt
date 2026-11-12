package sk.styk.martin.apkanalyzer.detektrules

import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

private const val CONTRACT = "AGENTS.md > Architecture > ViewModels"

class ViewModelContract(config: Config) :
    Rule(
        config,
        "ViewModels extend ViewModel() directly, are internal, expose only state, events and onAction, " +
            "send events through a buffered Channel and never touch Navigator.",
    ) {

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val importsNavigator = importDirective.importedFqName?.shortName()?.asString() == "Navigator"
        if (importsNavigator && importDirective.containingKtFile.declarations.any { it is KtClass && it.isViewModel() }) {
            report(
                importDirective,
                "A ViewModel never touches Navigator. Model navigation as an Action and an Event ($CONTRACT).",
            )
        }
    }

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (!klass.isViewModel()) return
        checkSuperType(klass)
        checkVisibility(klass)
        klass.body?.declarations.orEmpty().forEach(::checkMember)
        checkEvents(klass)
    }

    private fun KtClass.isViewModel(): Boolean {
        val superTypes = superTypeNames()
        return "ViewModel" in superTypes || "AndroidViewModel" in superTypes || hasAnnotation("HiltViewModel")
    }

    private fun checkSuperType(klass: KtClass) {
        if ("ViewModel" !in klass.superTypeNames()) {
            report(klass.nameIdentifier ?: klass, "Extend ViewModel() directly, with no base class ($CONTRACT).")
        }
    }

    private fun checkVisibility(klass: KtClass) {
        if (!klass.isHiddenFromOtherModules()) {
            report(klass.nameIdentifier ?: klass, "Declare the ViewModel internal; no other module uses it directly.")
        }
    }

    private fun checkMember(declaration: KtDeclaration) {
        if (declaration.isExemptMember()) return
        report(
            declaration,
            "A ViewModel exposes only `val state: StateFlow<...>`, `val events` and `fun onAction(action)`. " +
                "Make '${declaration.name.orEmpty()}' private or route it through onAction ($CONTRACT).",
        )
    }

    private fun KtDeclaration.isExemptMember(): Boolean = this is KtAnonymousInitializer || isPrivateDeclaration() || isOverride() || isAllowedMember()

    private fun KtDeclaration.isAllowedMember(): Boolean = when (this) {
        is KtProperty -> name == "events" || (name == "state" && typeReference?.baseTypeName().let { it == null || it == "StateFlow" })
        is KtNamedFunction -> name == "onAction" && valueParameters.size == 1
        is KtClass -> isInterface() && hasAnnotation("AssistedFactory")
        else -> false
    }

    private fun checkEvents(klass: KtClass) {
        PsiTreeUtil.collectElementsOfType(klass, KtCallExpression::class.java).forEach { call ->
            when (call.calleeName()) {
                "MutableSharedFlow" -> report(
                    call,
                    "Send one-shot events through Channel<Event>(Channel.BUFFERED) exposed with receiveAsFlow() ($CONTRACT).",
                )

                "Channel" -> if (!call.isBufferedChannel()) {
                    report(call, "Create the event channel with Channel.BUFFERED ($CONTRACT).")
                }
            }
        }
        PsiTreeUtil.collectElementsOfType(klass, KtNameReferenceExpression::class.java)
            .filter { it.getReferencedName() == "Navigator" }
            .forEach { report(it, "A ViewModel never touches Navigator. Model navigation as an Action and an Event ($CONTRACT).") }
    }

    private fun KtCallExpression.isBufferedChannel(): Boolean = valueArguments.singleOrNull()?.getArgumentExpression()?.let { argument ->
        (argument is KtDotQualifiedExpression && argument.text.endsWith("Channel.BUFFERED")) ||
            argument.text == "BUFFERED"
    } == true
}
