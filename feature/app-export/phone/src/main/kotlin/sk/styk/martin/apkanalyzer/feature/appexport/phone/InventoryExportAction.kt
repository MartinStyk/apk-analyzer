package sk.styk.martin.apkanalyzer.feature.appexport.phone

import android.net.Uri
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumnGroup
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryPreset
import sk.styk.martin.apkanalyzer.core.appexport.format.InventoryFormat
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

sealed interface InventoryExportAction {
    sealed interface AppSelectionAction : InventoryExportAction
    sealed interface ColumnSelectionAction : InventoryExportAction

    data object Back : InventoryExportAction
    data object Next : InventoryExportAction
    data class GoToStep(val step: InventoryExportStep) : InventoryExportAction

    data class SearchQueryChanged(val query: String) : AppSelectionAction
    data class AppToggled(val packageName: PackageName) : AppSelectionAction
    data object VisibleAppsToggled : AppSelectionAction
    data object EditFilters : AppSelectionAction

    data class PresetSelected(val preset: InventoryPreset) : ColumnSelectionAction
    data class ColumnToggled(val column: InventoryColumn) : ColumnSelectionAction
    data class ColumnGroupToggled(val group: InventoryColumnGroup) : ColumnSelectionAction
    data object AllColumnsToggled : ColumnSelectionAction
    data object OpenUsageAccessSettings : ColumnSelectionAction

    data object ShowIntro : InventoryExportAction
    data object DismissIntro : InventoryExportAction

    data class FormatSelected(val format: InventoryFormat) : InventoryExportAction
    data object OpenFormatReference : InventoryExportAction

    data object Save : InventoryExportAction
    data class SaveTo(val destination: Uri, val format: InventoryFormat) : InventoryExportAction
    data object Share : InventoryExportAction
    data object DocumentPickerUnavailable : InventoryExportAction
    data object ShareUnavailable : InventoryExportAction

    data object CancelExport : InventoryExportAction
    data object DismissStopConfirmation : InventoryExportAction
    data object ConfirmStopAndBack : InventoryExportAction
}
