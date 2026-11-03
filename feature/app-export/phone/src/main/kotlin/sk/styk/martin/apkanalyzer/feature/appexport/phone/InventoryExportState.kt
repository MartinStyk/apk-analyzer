package sk.styk.martin.apkanalyzer.feature.appexport.phone

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumnShape
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryPreset
import sk.styk.martin.apkanalyzer.core.appexport.format.InventoryFormat
import sk.styk.martin.apkanalyzer.feature.appexport.phone.apps.AppSelectionState

enum class InventoryExportStep {
    Apps,
    Columns,
    Export,
}

@Immutable
data class InventoryExportState(
    val step: InventoryExportStep = InventoryExportStep.Apps,
    val steps: List<InventoryExportStep> = InventoryExportStep.entries,
    val apps: AppSelectionState = AppSelectionState(),
    val columns: Set<InventoryColumn> = InventoryPreset.Default.columns,
    val isUsageAccessGranted: Boolean = true,
    val format: InventoryFormat = InventoryFormat.Csv,
    val preview: InventoryPreviewState = InventoryPreviewState.Loading,
    val progress: InventoryExportProgressState = InventoryExportProgressState.Idle,
    val showStopConfirmation: Boolean = false,
    val showIntro: Boolean = false,
) {
    val previousStep: InventoryExportStep?
        get() = steps.getOrNull(steps.indexOf(step) - 1)

    val nextStep: InventoryExportStep?
        get() = steps.getOrNull(steps.indexOf(step) + 1)

    val exportedColumns: List<InventoryColumn>
        get() = columns.exportedAs(format)

    val omittedColumns: List<InventoryColumn>
        get() = InventoryColumn.entries.filter { it in columns && it !in exportedColumns }

    val exportedColumnCount: Int
        get() = exportedColumns.size

    val preset: InventoryPreset?
        get() = InventoryPreset.matching(columns)

    val hasStructuredColumns: Boolean
        get() = columns.any { it.shape == InventoryColumnShape.Structured }

    val needsAppAnalysis: Boolean
        get() = columns.any { it.requiresAppAnalysis }

    val needsUsageAccess: Boolean
        get() = !isUsageAccessGranted && columns.any { it.requiresUsageAccess }

    val canExport: Boolean
        get() = apps.summary.hasSelection && exportedColumnCount > 0

    val canProceed: Boolean
        get() = when (step) {
            InventoryExportStep.Apps -> apps.summary.hasSelection
            InventoryExportStep.Columns -> columns.isNotEmpty()
            InventoryExportStep.Export -> false
        }

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

internal fun Set<InventoryColumn>.exportedAs(format: InventoryFormat): List<InventoryColumn> =
    InventoryColumn.entries.filter { it in this && format.supports(it) }
