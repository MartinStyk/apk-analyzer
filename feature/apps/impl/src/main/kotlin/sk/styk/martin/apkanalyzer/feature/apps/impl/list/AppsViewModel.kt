package sk.styk.martin.apkanalyzer.feature.apps.impl.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
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
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.components.AppDataPermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.AppFilterRepository
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.FilterAppsUseCase
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    recentlyViewedAppsRepository: RecentlyViewedAppsRepository,
    private val appFilterRepository: AppFilterRepository,
    filterApps: FilterAppsUseCase,
    private val usageStatsRepository: UsageStatsRepository,
    private val storageStatsRepository: StorageStatsRepository,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val sortType = MutableStateFlow(SortType.Name)
    private val sortAscending = MutableStateFlow(true)
    private val sortPermissionRationale = MutableStateFlow<AppDataPermission?>(null)

    private val eventChannel = Channel<AppsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val recentsFlow = recentlyViewedAppsRepository.recents()
        .map { apps ->
            if (apps.isEmpty()) {
                RecentsState.NoRecents
            } else {
                RecentsState.Content(apps.map { it.toListItem() }.toImmutableList())
            }
        }

    private val filteredItemsFlow = combine(
        installedAppsRepository.apps(),
        appFilterRepository.filter,
    ) { rawApps, filter ->
        val filtered = filterApps(rawApps, filter)
        filtered.map { it.toListItem() }.toImmutableList() to filter
    }.flowOn(dispatcherProvider.default())

    val state = combine(
        filteredItemsFlow,
        recentsFlow,
        sortType,
        sortAscending,
        sortPermissionRationale,
    ) { (filteredApps, filter), recents, sort, ascending, rationale ->
        val (effectiveSortType, effectiveAscending) = when {
            filter.isRecentInstallActive -> SortType.InstallDate to false
            filter.isRecentUpdateActive -> SortType.LastUpdated to false
            filter.isRecentlyUsedActive -> SortType.LastUsed to false
            filter.isUnusedFilterActive -> SortType.LastUsed to true
            filter.isLargeTotalFilterActive -> SortType.TotalSize to false
            else -> sort to ascending
        }

        val sortedItems = filteredApps
            .sortedWith(effectiveSortType.comparator(effectiveAscending))
            .toImmutableList()

        AppsState(
            apps = AppListState.Content(apps = sortedItems),
            recents = if (!filter.isActive) recents else RecentsState.NoRecents,
            sortType = effectiveSortType,
            sortAscending = effectiveAscending,
            activeFilter = filter,
            sortPermissionRationale = rationale,
        )
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppsState())

    fun onAction(action: AppsAction) {
        when (action) {
            is AppsAction.SortTypeSelected -> {
                val missingPermission = when {
                    action.sortType == SortType.TotalSize && !storageStatsRepository.isPermissionGranted.value -> AppDataPermission.StorageAccess
                    action.sortType == SortType.LastUsed && !usageStatsRepository.isPermissionGranted.value -> AppDataPermission.UsageAccess
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

            is AppsAction.ClearAllFilters -> appFilterRepository.clear()

            is AppsAction.DismissSortPermissionRationale -> sortPermissionRationale.value = null

            is AppsAction.OpenSortPermissionSettings -> {
                sortPermissionRationale.value = null
                eventChannel.trySend(AppsEvent.OpenUsageAccessSettings)
            }
        }
    }

    private fun SortType.comparator(ascending: Boolean): Comparator<AppListItem> {
        val base: Comparator<AppListItem> = when (this) {
            SortType.Name -> {
                val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.SECONDARY }
                Comparator { a, b -> collator.compare(a.applicationName, b.applicationName) }
            }

            SortType.ApkSize -> compareBy { it.apkSize }

            SortType.TotalSize -> Comparator { a, b ->
                when {
                    a.totalSize == null && b.totalSize == null -> 0
                    a.totalSize == null -> -1
                    b.totalSize == null -> 1
                    else -> a.totalSize.compareTo(b.totalSize)
                }
            }

            SortType.InstallDate -> compareBy { it.installTime }

            SortType.TargetSdk -> compareBy { it.targetSdk }

            SortType.LastUpdated -> compareBy { it.lastUpdateTime }

            SortType.LastUsed -> Comparator { a, b ->
                when {
                    a.lastUsedTime == null && b.lastUsedTime == null -> 0
                    a.lastUsedTime == null -> -1
                    b.lastUsedTime == null -> 1
                    else -> a.lastUsedTime.compareTo(b.lastUsedTime)
                }
            }
        }
        return if (ascending) base else base.reversed()
    }

    private fun InstalledApp.toListItem() = AppListItem(
        packageName = packageName,
        applicationName = applicationName,
        targetSdk = targetSdk,
        apkSize = apkSize,
        totalSize = totalSize,
        installTime = installTime,
        lastUpdateTime = lastUpdateTime,
        lastUsedTime = lastUsedTime,
        source = source,
    )
}
