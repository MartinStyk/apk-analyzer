package sk.styk.martin.apkanalyzer.feature.apps.phone.inventory

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttributeGroup
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.feature.apps.phone.list.AppListState

enum class InventoryExportStep {
    Apps,
    Columns,
    Export,
}

@Immutable
data class InventoryExportState(
    val step: InventoryExportStep = InventoryExportStep.Apps,
    val apps: AppListState = AppListState.Loading,
    val isFilterActive: Boolean = false,
    val selectedAttributes: Set<InventoryAttribute> = InventoryAttribute.Defaults,
    val isUsageAccessGranted: Boolean = true,
    val format: InventoryFormat = InventoryFormat.Csv,
    val preview: InventoryPreviewState = InventoryPreviewState.Loading,
    val progress: InventoryExportProgressState = InventoryExportProgressState.Idle,
) {
    val appCount: Int
        get() = (apps as? AppListState.Content)?.apps?.size ?: 0

    val canContinue: Boolean
        get() = when (step) {
            InventoryExportStep.Apps -> appCount > 0
            InventoryExportStep.Columns -> selectedAttributes.isNotEmpty()
            InventoryExportStep.Export -> appCount > 0 && selectedAttributes.isNotEmpty()
        }

    val needsAppAnalysis: Boolean
        get() = selectedAttributes.any { it.requiresAppAnalysis }

    val needsUsageAccess: Boolean
        get() = !isUsageAccessGranted && selectedAttributes.any { it.requiresUsageAccess }

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

internal fun InventoryAttributeGroup.attributes(): List<InventoryAttribute> = InventoryAttribute.entries.filter { it.group == this }
