package sk.styk.martin.apkanalyzer.core.appexport

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp

data class InventoryExportRequest(
    val apps: List<InstalledApp>,
    val attributes: List<InventoryAttribute>,
    val format: InventoryFormat,
)

data class InventoryPreview(
    val content: String,
    val previewedAppCount: Int,
    val isTruncated: Boolean,
)

sealed interface InventoryExportProgress {
    data class Running(val processedApps: Int, val totalApps: Int) : InventoryExportProgress

    data class Completed(
        val displayName: String,
        val exportedAppCount: Int,
        val incompleteAppCount: Int,
    ) : InventoryExportProgress

    data object Failed : InventoryExportProgress
}

interface InventoryExportManager {
    suspend fun preview(request: InventoryExportRequest): Result<InventoryPreview>
    fun export(request: InventoryExportRequest, destination: Uri): Flow<InventoryExportProgress>
    suspend fun createShareDestination(fileName: String): Result<Uri>
}
