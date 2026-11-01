package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.export

import android.net.Uri
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportStep

sealed interface ExportStepAction {
    data object NavigateBack : ExportStepAction
    data class StepSelected(val step: InventoryExportStep) : ExportStepAction
    data class FormatSelected(val format: InventoryFormat) : ExportStepAction
    data object Save : ExportStepAction
    data class SaveTo(val destination: Uri, val format: InventoryFormat) : ExportStepAction
    data object DocumentPickerUnavailable : ExportStepAction
    data object Share : ExportStepAction
    data object ShareUnavailable : ExportStepAction
    data object CancelExport : ExportStepAction
}
