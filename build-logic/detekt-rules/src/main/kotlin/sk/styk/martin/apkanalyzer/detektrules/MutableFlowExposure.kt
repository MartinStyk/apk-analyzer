package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty

class MutableFlowExposure(config: Config) :
    Rule(
        config,
        "Mutable flows and channels stay private; expose read-only state through an explicit backing field.",
    ) {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName()
        if (name == "asStateFlow" || name == "asSharedFlow") {
            report(
                expression,
                "Replace the private mutable flow and $name() with an explicit backing field: " +
                    "`val state: StateFlow<T>` followed by `field = MutableStateFlow(initial)` (AGENTS.md > Architecture > ViewModels).",
            )
        }
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (property.isLocal || property.isPrivateDeclaration() || property.isOverride()) return
        val typeName = property.typeReference?.baseTypeName() ?: (property.initializer as? KtCallExpression)?.calleeName()
        if (typeName in MUTABLE_TYPES) {
            report(
                property,
                "Don't expose '${property.name.orEmpty()}' as ${typeName.orEmpty()}. Keep it private and expose a read-only type, using an " +
                    "explicit backing field for state (AGENTS.md > Architecture > ViewModels).",
            )
        }
    }

    private companion object {
        val MUTABLE_TYPES = setOf("MutableStateFlow", "MutableSharedFlow", "Channel")
    }
}
