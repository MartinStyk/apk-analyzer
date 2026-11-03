package sk.styk.martin.apkanalyzer.feature.appexport.phone

import android.net.Uri
import sk.styk.martin.apkanalyzer.core.appexport.format.InventoryFormat

sealed interface InventoryExportEvent {
    data object NavigateBack : InventoryExportEvent
    data object NavigateToFilter : InventoryExportEvent
    data object OpenUsageAccessSettings : InventoryExportEvent
    data class OpenFormatReference(val url: String) : InventoryExportEvent
    data class CreateDocument(val format: InventoryFormat, val fileName: String) : InventoryExportEvent
    data class ShareFile(val uri: Uri, val mimeType: String) : InventoryExportEvent
    data class ShowFeedback(val feedback: InventoryExportFeedback) : InventoryExportEvent
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
