package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.columns

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute

@Immutable
data class ColumnsStepState(val selectedAttributes: Set<InventoryAttribute> = InventoryAttribute.Defaults, val isUsageAccessGranted: Boolean = true) {
    val canContinue: Boolean
        get() = selectedAttributes.isNotEmpty()

    val needsAppAnalysis: Boolean
        get() = selectedAttributes.any { it.requiresAppAnalysis }

    val needsUsageAccess: Boolean
        get() = !isUsageAccessGranted && selectedAttributes.any { it.requiresUsageAccess }
}
