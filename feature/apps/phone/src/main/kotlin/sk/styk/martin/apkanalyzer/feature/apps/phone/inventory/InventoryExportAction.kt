package sk.styk.martin.apkanalyzer.feature.apps.phone.inventory

import android.net.Uri
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttributeGroup
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

sealed interface InventoryExportAction {
    data object NavigateBack : InventoryExportAction
    data object Continue : InventoryExportAction
    data class StepSelected(val step: InventoryExportStep) : InventoryExportAction
    data object EditFilters : InventoryExportAction
    data object IncludeAllApps : InventoryExportAction
    data class AppClicked(val packageName: PackageName) : InventoryExportAction
    data class AttributeToggled(val attribute: InventoryAttribute) : InventoryExportAction
    data class GroupToggled(val group: InventoryAttributeGroup) : InventoryExportAction
    data object AllAttributesToggled : InventoryExportAction
    data object OpenUsageAccessSettings : InventoryExportAction
    data class FormatSelected(val format: InventoryFormat) : InventoryExportAction
    data object Save : InventoryExportAction
    data class SaveTo(val destination: Uri, val format: InventoryFormat) : InventoryExportAction
    data object DocumentPickerUnavailable : InventoryExportAction
    data object Share : InventoryExportAction
    data object ShareUnavailable : InventoryExportAction
    data object CancelExport : InventoryExportAction
}
