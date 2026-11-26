package sk.styk.martin.apkanalyzer.feature.apps.tv.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import sk.styk.martin.apkanalyzer.core.apps.api.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.ActivityQuickFilter
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.AppFilterRepository
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.AppFilterState
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.PermissionPreset
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.QuickFilter
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.filterBy
import sk.styk.martin.apkanalyzer.core.apps.api.sdkversion.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.apps.api.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import javax.inject.Inject

@HiltViewModel
internal class TvAppFilterViewModel @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    private val appFilterRepository: AppFilterRepository,
    usageStatsRepository: UsageStatsRepository,
    sdkVersionResolver: SdkVersionResolver,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    val state: StateFlow<TvAppFilterState> = combine(
        installedAppsRepository.apps(),
        appFilterRepository.filter,
        usageStatsRepository.isPermissionGranted,
    ) { apps, filter, usageAccessGranted ->
        val sourceCounts = apps.groupingBy { it.source }.eachCount()
        val sdkCounts = apps.groupingBy { it.targetSdk }.eachCount()
        TvAppFilterState(
            matchingAppCount = apps.filterBy(filter).size,
            isActive = filter.isActive,
            sources = AppSource.entries
                .filter { (sourceCounts[it] ?: 0) > 0 }
                .map { TvCountedOption(it, selected = it in filter.selectedSources, appCount = sourceCounts.getValue(it)) },
            installQuickFilters = listOf(QuickFilter.RecentlyInstalled, QuickFilter.RecentlyUpdated)
                .map { TvFilterOption(it, selected = filter.isQuickFilterActive(it)) },
            usageQuickFilters = if (usageAccessGranted) {
                listOf(TvFilterOption(QuickFilter.Large, selected = filter.isLargeTotalFilterActive))
            } else {
                listOf()
            },
            activityFilters = if (usageAccessGranted) {
                ActivityQuickFilter.entries.map { TvFilterOption(it, selected = filter.activeActivityQuickFilter == it) }
            } else {
                listOf()
            },
            permissionPresets = PermissionPreset.all
                .map { TvFilterOption(it, selected = filter.selectedPermissions == it.permissions) },
            sdkVersions = sdkCounts.keys.sortedDescending().map { sdk ->
                TvSdkOption(
                    sdk = sdk,
                    versionName = sdkVersionResolver.resolveVersion(sdk),
                    selected = sdk in filter.selectedSdkVersions,
                    appCount = sdkCounts.getValue(sdk),
                )
            },
        )
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvAppFilterState())

    fun onAction(action: TvAppFilterAction) {
        val current = appFilterRepository.filter.value
        when (action) {
            is TvAppFilterAction.SourceToggled ->
                appFilterRepository.update(current.copy(selectedSources = current.selectedSources.toggle(action.source)))

            is TvAppFilterAction.QuickFilterToggled -> appFilterRepository.toggleQuickFilter(action.filter)

            is TvAppFilterAction.ActivityFilterToggled -> appFilterRepository.selectActivityQuickFilter(
                action.filter.takeUnless { current.activeActivityQuickFilter == it },
            )

            is TvAppFilterAction.PermissionPresetToggled -> appFilterRepository.update(
                current.copy(
                    selectedPermissions = if (current.selectedPermissions == action.preset.permissions) setOf() else action.preset.permissions,
                    permissionMatchAll = false,
                ),
            )

            is TvAppFilterAction.SdkVersionToggled ->
                appFilterRepository.update(current.copy(selectedSdkVersions = current.selectedSdkVersions.toggle(action.sdk)))

            TvAppFilterAction.ClearAll -> appFilterRepository.clear()
        }
    }
}

private fun AppFilterState.isQuickFilterActive(filter: QuickFilter): Boolean = when (filter) {
    QuickFilter.SensitivePermissions -> isSensitivePermissionsFilterActive
    QuickFilter.Large -> isLargeTotalFilterActive
    QuickFilter.RecentlyInstalled -> isRecentInstallActive
    QuickFilter.RecentlyUpdated -> isRecentUpdateActive
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
