package sk.styk.martin.apkanalyzer.core.appexport.api

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.appexport.api.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.api.format.InventoryFormat
import sk.styk.martin.apkanalyzer.core.apps.api.model.InstalledApp

interface InventoryExportManager {
    suspend fun preview(request: InventoryExportRequest): Result<InventoryPreview>
    fun export(request: InventoryExportRequest, destination: InventoryDestination): Flow<InventoryExportProgress>
}

data class InventoryExportRequest(
    val apps: List<InstalledApp>,
    val columns: List<InventoryColumn>,
    val format: InventoryFormat,
)

sealed interface InventoryDestination {
    data class Document(val uri: Uri) : InventoryDestination

    data object Share : InventoryDestination
}

data class InventoryPreview(
    val content: String,
    val previewedAppCount: Int,
    val isTruncated: Boolean,
)

sealed interface InventoryExportProgress {
    data class Running(val processedApps: Int, val totalApps: Int) : InventoryExportProgress

    data class Completed(
        val uri: Uri,
        val displayName: String,
        val exportedAppCount: Int,
        val incompleteAppCount: Int,
    ) : InventoryExportProgress

    data object Failed : InventoryExportProgress
}
