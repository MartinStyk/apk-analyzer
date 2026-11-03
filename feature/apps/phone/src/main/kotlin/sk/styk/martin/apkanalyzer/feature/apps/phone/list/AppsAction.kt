package sk.styk.martin.apkanalyzer.feature.apps.phone.list

import sk.styk.martin.apkanalyzer.core.apps.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.AppDataPermission

sealed interface AppsAction {
    data class SortTypeSelected(val sortType: AppSortType) : AppsAction
    data class AppClicked(val packageName: PackageName) : AppsAction
    data object SearchClicked : AppsAction
    data object OpenSettings : AppsAction
    data object FilterClicked : AppsAction
    data object ExportClicked : AppsAction
    data object ClearAllFilters : AppsAction
    data object DismissSortPermissionRationale : AppsAction
    data class OpenSortPermissionSettings(val permission: AppDataPermission) : AppsAction
}
