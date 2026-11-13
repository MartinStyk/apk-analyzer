package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeReference

private const val VIEW_MODEL_CONTRACT = "AGENTS.md > Architecture > ViewModels"

class ContractTypeShape(config: Config) :
    Rule(
        config,
        "Action and Event types are sealed interfaces; State is a sealed interface or an @Immutable data class " +
            "that holds no lambdas or mutable collections.",
    ) {

    override fun visit(root: KtFile) {
        super.visit(root)
        root.declarations.filterIsInstance<KtClass>().forEach(::checkContractType)
    }

    private fun checkContractType(klass: KtClass) {
        val name = klass.name ?: return
        val target = klass.nameIdentifier ?: klass
        val isSealedInterface = klass.isInterface() && klass.isSealed()
        when {
            name.endsWith("Action") && !isSealedInterface ->
                report(target, "Declare '$name' as a sealed interface ($VIEW_MODEL_CONTRACT).")

            name.endsWith("Event") && !name.endsWith("AnalyticsEvent") && !isSealedInterface ->
                report(target, "Declare '$name' as a sealed interface ($VIEW_MODEL_CONTRACT).")

            name.endsWith("State") -> checkState(klass, isSealedInterface)
        }
    }

    private fun checkState(klass: KtClass, isSealedInterface: Boolean) {
        val target = klass.nameIdentifier ?: klass
        val isImmutableData = klass.isData() && klass.hasAnnotation("Immutable")
        if (!isSealedInterface && !isImmutableData && !klass.isEnum()) {
            report(target, "Declare '${klass.name.orEmpty()}' as a sealed interface or an @Immutable data class ($VIEW_MODEL_CONTRACT).")
        }
        klass.statePropertyParameters().forEach { parameter ->
            val type = parameter.typeReference ?: return@forEach
            when {
                type.isFunctionType() -> report(parameter, "State never holds lambdas; route the callback through an Action ($VIEW_MODEL_CONTRACT).")
                type.isMutableCollection() -> report(parameter, "State holds read-only List, Set and Map only (AGENTS.md > Compose).")
            }
        }
    }

    private fun KtClass.statePropertyParameters(): List<KtParameter> {
        val nested = body?.declarations.orEmpty().filterIsInstance<KtClass>().flatMap { it.primaryConstructorParameters }
        return primaryConstructorParameters + nested
    }

    private fun KtTypeReference.isFunctionType(): Boolean {
        val element = typeElement
        return element is KtFunctionType || (element is KtNullableType && element.innerType is KtFunctionType)
    }

    private fun KtTypeReference.isMutableCollection(): Boolean = baseTypeName() in MUTABLE_COLLECTIONS

    private companion object {
        val MUTABLE_COLLECTIONS = setOf(
            "MutableList",
            "MutableSet",
            "MutableMap",
            "MutableCollection",
            "ArrayList",
            "HashMap",
            "HashSet",
            "LinkedHashMap",
            "LinkedHashSet",
            "SnapshotStateList",
            "SnapshotStateMap",
        )
    }
}
