package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.export

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat

@Immutable
data class ExportStepState(
    val appCount: Int = 0,
    val selectedAttributes: Set<InventoryAttribute> = InventoryAttribute.Defaults,
    val format: InventoryFormat = InventoryFormat.Csv,
    val preview: InventoryPreviewState = InventoryPreviewState.Loading,
    val progress: InventoryExportProgressState = InventoryExportProgressState.Idle,
) {
    val canExport: Boolean
        get() = appCount > 0 && selectedAttributes.isNotEmpty()

    val isExporting: Boolean
        get() = progress is InventoryExportProgressState.Running
}

sealed interface InventoryPreviewState {
    data object Loading : InventoryPreviewState

    data class Content(
        val text: String,
        val previewedAppCount: Int,
        val isTruncated: Boolean,
    ) : InventoryPreviewState

    data object Unavailable : InventoryPreviewState
}

sealed interface InventoryExportProgressState {
    data object Idle : InventoryExportProgressState

    data class Running(val processedApps: Int, val totalApps: Int) : InventoryExportProgressState {
        val fraction: Float
            get() = if (totalApps == 0) 0f else processedApps.toFloat() / totalApps
    }
}
