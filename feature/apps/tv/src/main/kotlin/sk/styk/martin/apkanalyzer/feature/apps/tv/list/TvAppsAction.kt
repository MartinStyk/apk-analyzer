package sk.styk.martin.apkanalyzer.feature.apps.tv.list

import sk.styk.martin.apkanalyzer.core.apps.api.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal sealed interface TvAppsAction {
    data class AppClicked(val packageName: PackageName) : TvAppsAction
    data class SortTypeSelected(val sortType: AppSortType) : TvAppsAction
    data object SortClicked : TvAppsAction
    data object OverlayDismissed : TvAppsAction
    data object OpenUsageAccessSettings : TvAppsAction
    data object ScreenResumed : TvAppsAction
    data object SearchClicked : TvAppsAction
    data object FiltersClicked : TvAppsAction
    data object ClearFilters : TvAppsAction
}
