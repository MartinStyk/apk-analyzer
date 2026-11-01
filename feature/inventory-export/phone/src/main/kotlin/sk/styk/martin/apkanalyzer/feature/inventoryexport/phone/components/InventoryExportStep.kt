package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components

import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.R

enum class InventoryExportStep(@StringRes val labelRes: Int) {
    Apps(R.string.inventory_export_step_apps),
    Columns(R.string.inventory_export_step_columns),
    Export(R.string.inventory_export_step_export),
}
