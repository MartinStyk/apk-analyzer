package sk.styk.martin.apkanalyzer.feature.appexport.phone.apps

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

@Immutable
data class AppSelectionSummary(
    val isLoaded: Boolean = false,
    val candidateCount: Int = 0,
    val selectedCount: Int = 0,
    val isFilterActive: Boolean = false,
) {
    val hasSelection: Boolean
        get() = selectedCount > 0

    val leftOutCount: Int
        get() = candidateCount - selectedCount
}

@Immutable
data class AppSelectionState(
    val summary: AppSelectionSummary = AppSelectionSummary(),
    val items: InventoryAppList = InventoryAppList.Loading,
    val query: String = "",
) {
    val areAllVisibleSelected: Boolean
        get() = items is InventoryAppList.Content && items.apps.isNotEmpty() && items.apps.all { it.isSelected }
}

sealed interface InventoryAppList {
    data object Loading : InventoryAppList

    @Immutable
    data class Content(val apps: List<InventoryAppItem>) : InventoryAppList
}

@Immutable
data class InventoryAppItem(
    val packageName: PackageName,
    val applicationName: String,
    val isSelected: Boolean,
)
