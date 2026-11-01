package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.columns

import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportStep

sealed interface ColumnsStepEvent {
    data object NavigateBack : ColumnsStepEvent
    data object NavigateToExport : ColumnsStepEvent
    data class NavigateToStep(val step: InventoryExportStep) : ColumnsStepEvent
    data object OpenUsageAccessSettings : ColumnsStepEvent
}
