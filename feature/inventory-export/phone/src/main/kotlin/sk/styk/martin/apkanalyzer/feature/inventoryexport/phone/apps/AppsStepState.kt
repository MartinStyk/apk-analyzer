package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.apps

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

@Immutable
data class AppsStepState(val apps: InventoryAppList = InventoryAppList.Loading, val isFilterActive: Boolean = false) {
    val canContinue: Boolean
        get() = apps is InventoryAppList.Content && apps.apps.isNotEmpty()
}

sealed interface InventoryAppList {
    data object Loading : InventoryAppList

    @Immutable
    data class Content(val apps: List<InventoryAppItem>) : InventoryAppList
}

@Immutable
data class InventoryAppItem(val packageName: PackageName, val applicationName: String)
