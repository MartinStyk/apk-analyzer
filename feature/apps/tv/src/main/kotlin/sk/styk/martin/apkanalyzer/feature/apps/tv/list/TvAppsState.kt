package sk.styk.martin.apkanalyzer.feature.apps.tv.list

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.api.sorting.AppSortType
import sk.styk.martin.apkanalyzer.feature.apps.tv.components.TvAppItem

@Immutable
internal data class TvAppsState(
    val apps: TvAppListState = TvAppListState.Loading,
    val recents: List<TvAppItem> = listOf(),
    val sortType: AppSortType = AppSortType.Name,
    val sortAscending: Boolean = true,
    val sortOptions: List<AppSortType> = listOf(),
    val overlay: TvAppsOverlay = TvAppsOverlay.None,
    val activeFilterCount: Int = 0,
)

internal sealed interface TvAppListState {
    data object Loading : TvAppListState

    @Immutable
    data class Content(val apps: List<TvAppItem>) : TvAppListState
}

internal sealed interface TvAppsOverlay {
    data object None : TvAppsOverlay
    data object SortPanel : TvAppsOverlay
    data object UsageAccessRationale : TvAppsOverlay
}
