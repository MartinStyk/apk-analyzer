package sk.styk.martin.apkanalyzer.feature.apps.tv.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.api.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.AppFilterRepository
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.AppFilterState
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.filterBy
import sk.styk.martin.apkanalyzer.core.apps.api.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.api.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.apps.api.sorting.comparator
import sk.styk.martin.apkanalyzer.core.apps.api.storagestats.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.api.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.feature.apps.tv.components.toTvAppItem
import javax.inject.Inject

@HiltViewModel
internal class TvAppsViewModel @Inject constructor(
    private val installedAppsRepository: InstalledAppsRepository,
    recentlyViewedAppsRepository: RecentlyViewedAppsRepository,
    private val appFilterRepository: AppFilterRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val storageStatsRepository: StorageStatsRepository,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val sort = MutableStateFlow(DefaultSort)
    private val overlay = MutableStateFlow<TvAppsOverlay>(TvAppsOverlay.None)
    private var sortAwaitingUsageAccess: AppSortType? = null

    private val eventChannel = Channel<TvAppsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val filteredAppsFlow = combine(
        installedAppsRepository.apps(),
        appFilterRepository.filter,
    ) { apps, filter -> FilteredApps(apps = apps.filterBy(filter), filter = filter) }

    val state: StateFlow<TvAppsState> = combine(
        filteredAppsFlow,
        recentlyViewedAppsRepository.recents(),
        sort,
        usageStatsRepository.isPermissionGranted,
        overlay,
    ) { filteredApps, recents, selectedSort, usageAccessGranted, currentOverlay ->
        val effectiveSort = selectedSort.takeIf { usageAccessGranted || !it.type.needsUsageAccess } ?: DefaultSort
        TvAppsState(
            apps = TvAppListState.Content(
                apps = filteredApps.apps
                    .sortedWith(effectiveSort.type.comparator(effectiveSort.ascending))
                    .map { it.toTvAppItem() },
            ),
            activeFilterCount = filteredApps.filter.activeCriteriaCount,
            recents = if (filteredApps.filter.isActive) listOf() else recents.map { it.toTvAppItem() },
            sortType = effectiveSort.type,
            sortAscending = effectiveSort.ascending,
            sortOptions = AppSortType.entries.filter { type ->
                !type.needsUsageAccess || usageAccessGranted || usageStatsRepository.canRequestPermission
            },
            overlay = currentOverlay,
        )
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvAppsState())

    fun onAction(action: TvAppsAction) {
        when (action) {
            is TvAppsAction.AppClicked -> eventChannel.trySend(TvAppsEvent.NavigateToAppDetail(action.packageName))

            TvAppsAction.SortClicked -> overlay.value = TvAppsOverlay.SortPanel

            is TvAppsAction.SortTypeSelected -> selectSort(action.sortType)

            TvAppsAction.OverlayDismissed -> {
                sortAwaitingUsageAccess = null
                overlay.value = TvAppsOverlay.None
            }

            TvAppsAction.OpenUsageAccessSettings -> {
                overlay.value = TvAppsOverlay.None
                eventChannel.trySend(TvAppsEvent.OpenUsageAccessSettings)
            }

            TvAppsAction.ScreenResumed -> refreshUsageAccess()

            TvAppsAction.SearchClicked -> eventChannel.trySend(TvAppsEvent.NavigateToSearch)

            TvAppsAction.FiltersClicked -> eventChannel.trySend(TvAppsEvent.NavigateToFilters)

            TvAppsAction.ClearFilters -> appFilterRepository.clear()
        }
    }

    private fun refreshUsageAccess() {
        viewModelScope.launch {
            if (!usageStatsRepository.isPermissionGranted.value) {
                usageStatsRepository.ensureLoaded()
                if (!usageStatsRepository.isPermissionGranted.value) return@launch
                val installedPackages = installedAppsRepository.apps().first().map { it.packageName }
                storageStatsRepository.ensureLoaded(installedPackages)
            }
            sortAwaitingUsageAccess?.let { awaitedSort ->
                sortAwaitingUsageAccess = null
                sort.value = SortSelection(awaitedSort, awaitedSort.naturallyAscending)
            }
        }
    }

    private fun selectSort(sortType: AppSortType) {
        if (sortType.needsUsageAccess && !usageStatsRepository.isPermissionGranted.value) {
            sortAwaitingUsageAccess = sortType
            overlay.value = TvAppsOverlay.UsageAccessRationale
            return
        }
        val current = sort.value
        sort.value = if (current.type == sortType) {
            current.copy(ascending = !current.ascending)
        } else {
            SortSelection(sortType, sortType.naturallyAscending)
        }
    }
}

private data class SortSelection(val type: AppSortType, val ascending: Boolean)

private val AppSortType.naturallyAscending: Boolean
    get() = when (this) {
        AppSortType.Name, AppSortType.TargetSdk -> true
        AppSortType.TotalSize, AppSortType.ApkSize, AppSortType.InstallDate, AppSortType.LastUpdated, AppSortType.LastUsed -> false
    }

private data class FilteredApps(val apps: List<InstalledApp>, val filter: AppFilterState)

private val AppFilterState.activeCriteriaCount: Int
    get() = listOf(
        selectedSources.isNotEmpty(),
        selectedSdkVersions.isNotEmpty(),
        selectedPermissions.isNotEmpty(),
        apkSizeRange != null,
        totalSizeRange != null,
        installTimeRange != null,
        updateTimeRange != null,
        unusedPeriod != null || recentlyUsedDays != null,
    ).count { it }

private val DefaultSort = SortSelection(AppSortType.Name, ascending = true)

private val AppSortType.needsUsageAccess: Boolean
    get() = this == AppSortType.TotalSize || this == AppSortType.LastUsed
