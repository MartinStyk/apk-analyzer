package sk.styk.martin.apkanalyzer.feature.apps.impl.list

import sk.styk.martin.apkanalyzer.core.common.model.AppDataPermission
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

sealed interface AppsAction {
    data class SortTypeSelected(val sortType: SortType) : AppsAction
    data class AppClicked(val packageName: PackageName) : AppsAction
    data object SearchClicked : AppsAction
    data object OpenSettings : AppsAction
    data object FilterClicked : AppsAction
    data object ClearAllFilters : AppsAction
    data object DismissSortPermissionRationale : AppsAction
    data class OpenSortPermissionSettings(val permission: AppDataPermission) : AppsAction
}
