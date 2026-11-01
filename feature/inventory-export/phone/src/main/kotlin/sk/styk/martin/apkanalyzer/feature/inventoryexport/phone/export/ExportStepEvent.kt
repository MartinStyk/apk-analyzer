package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.export

import android.net.Uri
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportStep

sealed interface ExportStepEvent {
    data object NavigateBack : ExportStepEvent
    data class NavigateToStep(val step: InventoryExportStep) : ExportStepEvent
    data class CreateDocument(val format: InventoryFormat, val fileName: String) : ExportStepEvent
    data class ShareFile(val uri: Uri, val mimeType: String) : ExportStepEvent
    data class ShowFeedback(val feedback: InventoryExportFeedback) : ExportStepEvent
}

sealed interface InventoryExportFeedback {
    data class Saved(
        val displayName: String,
        val appCount: Int,
        val incompleteAppCount: Int,
    ) : InventoryExportFeedback
    data class SharedWithGaps(val incompleteAppCount: Int) : InventoryExportFeedback
    data object Failed : InventoryExportFeedback
    data object DocumentPickerUnavailable : InventoryExportFeedback
    data object ShareUnavailable : InventoryExportFeedback
}
