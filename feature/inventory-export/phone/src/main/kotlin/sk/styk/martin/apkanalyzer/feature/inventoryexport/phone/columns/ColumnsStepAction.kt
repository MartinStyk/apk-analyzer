package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.columns

import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttributeGroup
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportStep

sealed interface ColumnsStepAction {
    data object NavigateBack : ColumnsStepAction
    data object Continue : ColumnsStepAction
    data class StepSelected(val step: InventoryExportStep) : ColumnsStepAction
    data class AttributeToggled(val attribute: InventoryAttribute) : ColumnsStepAction
    data class GroupToggled(val group: InventoryAttributeGroup) : ColumnsStepAction
    data object AllAttributesToggled : ColumnsStepAction
    data object OpenUsageAccessSettings : ColumnsStepAction
}
