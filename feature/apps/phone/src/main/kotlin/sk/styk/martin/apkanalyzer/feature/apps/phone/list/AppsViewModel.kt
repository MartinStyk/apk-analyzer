package sk.styk.martin.apkanalyzer.feature.apps.phone.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterRepository
import sk.styk.martin.apkanalyzer.core.apps.filtering.filterBy
import sk.styk.martin.apkanalyzer.core.apps.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.apps.sorting.comparator
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.configuration.ConfigurationKey
import sk.styk.martin.apkanalyzer.core.common.configuration.ConfigurationRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.combine
import sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.AppDataPermission
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    recentlyViewedAppsRepository: RecentlyViewedAppsRepository,
    configurationRepository: ConfigurationRepository,
    private val appFilterRepository: AppFilterRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val storageStatsRepository: StorageStatsRepository,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val sortType = MutableStateFlow(AppSortType.Name)
    private val sortAscending = MutableStateFlow(true)
    private val sortPermissionRationale = MutableStateFlow<AppDataPermission?>(null)

    private val eventChannel = Channel<AppsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val recentsFlow = recentlyViewedAppsRepository.recents()
        .map { apps ->
            if (apps.isEmpty()) {
                RecentsState.NoRecents
            } else {
                RecentsState.Content(apps.map { it.toListItem() })
            }
        }

    private val filteredItemsFlow = combine(
        installedAppsRepository.apps(),
        appFilterRepository.filter,
    ) { rawApps, filter ->
        rawApps.filterBy(filter) to filter
    }.flowOn(dispatcherProvider.default())

    val state = combine(
        filteredItemsFlow,
        recentsFlow,
        sortType,
        sortAscending,
        sortPermissionRationale,
        configurationRepository.observe(ConfigurationKey.InventoryExportEnabled),
    ) { (filteredApps, filter), recents, sort, ascending, rationale, isInventoryExportEnabled ->
        val (effectiveSortType, effectiveAscending) = when {
            filter.isRecentInstallActive -> AppSortType.InstallDate to false
            filter.isRecentUpdateActive -> AppSortType.LastUpdated to false
            filter.isRecentlyUsedActive -> AppSortType.LastUsed to false
            filter.isUnusedFilterActive -> AppSortType.LastUsed to true
            filter.isLargeTotalFilterActive -> AppSortType.TotalSize to false
            else -> sort to ascending
        }

        val sortedItems = filteredApps
            .sortedWith(effectiveSortType.comparator(effectiveAscending))
            .map { it.toListItem() }

        AppsState(
            apps = AppListState.Content(apps = sortedItems),
            recents = if (!filter.isActive) recents else RecentsState.NoRecents,
            sortType = effectiveSortType,
            sortAscending = effectiveAscending,
            activeFilter = filter,
            sortPermissionRationale = rationale,
            isInventoryExportEnabled = isInventoryExportEnabled,
        )
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppsState())

    fun onAction(action: AppsAction) {
        when (action) {
            is AppsAction.SortTypeSelected -> {
                val missingPermission = when {
                    action.sortType == AppSortType.TotalSize && !storageStatsRepository.isPermissionGranted.value -> AppDataPermission.StorageAccess
                    action.sortType == AppSortType.LastUsed && !usageStatsRepository.isPermissionGranted.value -> AppDataPermission.UsageAccess
                    else -> null
                }
                if (missingPermission != null) {
                    sortPermissionRationale.value = missingPermission
                    return
                }
                if (sortType.value == action.sortType) {
                    sortAscending.value = !sortAscending.value
                } else {
                    sortType.value = action.sortType
                    sortAscending.value = true
                }
            }

            is AppsAction.AppClicked -> {
                eventChannel.trySend(AppsEvent.NavigateToAppDetail(action.packageName))
            }

            is AppsAction.SearchClicked -> eventChannel.trySend(AppsEvent.NavigateToSearch)

            is AppsAction.OpenSettings -> eventChannel.trySend(AppsEvent.NavigateToSettings)

            is AppsAction.FilterClicked -> eventChannel.trySend(AppsEvent.NavigateToFilter)

            is AppsAction.ExportClicked -> eventChannel.trySend(AppsEvent.NavigateToInventoryExport)

            is AppsAction.ClearAllFilters -> appFilterRepository.clear()

            is AppsAction.DismissSortPermissionRationale -> sortPermissionRationale.value = null

            is AppsAction.OpenSortPermissionSettings -> {
                sortPermissionRationale.value = null
                eventChannel.trySend(AppsEvent.OpenUsageAccessSettings)
            }
        }
    }
}
