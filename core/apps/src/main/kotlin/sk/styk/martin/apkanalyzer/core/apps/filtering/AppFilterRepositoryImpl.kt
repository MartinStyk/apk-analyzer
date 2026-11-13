package sk.styk.martin.apkanalyzer.core.apps.filtering

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.apps.AppClassificationThresholds
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import sk.styk.martin.apkanalyzer.core.common.model.isSideloaded
import java.time.Instant
import javax.inject.Inject

internal class AppFilterRepositoryImpl @Inject constructor() : AppFilterRepository {
    final override val filter: StateFlow<AppFilterState>
        field = MutableStateFlow(AppFilterState())

    override val activeQuickFilters: Flow<Set<QuickFilter>>
        get() = filter.map { deriveActiveQuickFilters(it) }

    override val activeSourceQuickFilters: Flow<Set<SourceQuickFilter>>
        get() = filter.map { it.activeSourceQuickFilters }

    override val activeActivityQuickFilter: Flow<ActivityQuickFilter?>
        get() = filter.map { it.activeActivityQuickFilter }

    override fun update(filter: AppFilterState) {
        this.filter.value = filter
    }

    override fun clear() {
        filter.value = AppFilterState()
    }

    override fun toggleQuickFilter(filter: QuickFilter) {
        this.filter.update { current ->
            when (filter) {
                QuickFilter.SensitivePermissions -> toggleSensitivePermissions(current)
                QuickFilter.Large -> toggleLargeTotal(current)
                QuickFilter.RecentlyInstalled -> toggleRecentInstall(current)
                QuickFilter.RecentlyUpdated -> toggleRecentUpdate(current)
            }
        }
    }

    override fun toggleSourceQuickFilter(filter: SourceQuickFilter) {
        this.filter.update { current ->
            val sources = filter.appSources()
            val allSelected = sources.all { it in current.selectedSources }
            current.copy(
                selectedSources = if (allSelected) {
                    current.selectedSources - sources
                } else {
                    current.selectedSources + sources
                },
            )
        }
    }

    private fun SourceQuickFilter.appSources(): Set<AppSource> = when (this) {
        SourceQuickFilter.System -> setOf(AppSource.SystemPreinstalled)
        SourceQuickFilter.GooglePlay -> setOf(AppSource.GooglePlay)
        SourceQuickFilter.Sideloaded -> AppSource.entries.filter { it.isSideloaded }.toSet()
    }

    override fun selectActivityQuickFilter(filter: ActivityQuickFilter?) {
        this.filter.update { current ->
            current.copy(
                recentlyUsedDays = if (filter == ActivityQuickFilter.RecentlyUsed) AppClassificationThresholds.RECENTLY_USED_DAYS else null,
                unusedPeriod = if (filter == ActivityQuickFilter.Unused) UnusedAppsPeriod.SIX_MONTHS else null,
            )
        }
    }

    private fun toggleSensitivePermissions(current: AppFilterState): AppFilterState = current.copy(
        selectedPermissions = if (current.isSensitivePermissionsFilterActive) {
            setOf()
        } else {
            PermissionPreset.Sensitive.permissions.toSet()
        },
        permissionMatchAll = false,
    )

    private fun toggleLargeTotal(current: AppFilterState): AppFilterState = if (current.isLargeTotalFilterActive) {
        current.copy(totalSizeRange = null)
    } else {
        val min = AppClassificationThresholds.LARGE_SIZE
        val max = current.totalSizeRange?.max?.takeIf { it >= min } ?: Long.MAX_VALUE.bytes
        current.copy(totalSizeRange = AppSizeRange(min = min, max = max))
    }

    private fun toggleRecentInstall(current: AppFilterState): AppFilterState {
        val now = Instant.now()
        return current.copy(
            installTimeRange = if (current.isRecentInstallActive) {
                null
            } else {
                DateRange(start = now.minus(AppClassificationThresholds.RECENT_PERIOD), end = now)
            },
        )
    }

    private fun toggleRecentUpdate(current: AppFilterState): AppFilterState {
        val now = Instant.now()
        return current.copy(
            updateTimeRange = if (current.isRecentUpdateActive) {
                null
            } else {
                DateRange(start = now.minus(AppClassificationThresholds.RECENT_PERIOD), end = now)
            },
        )
    }
}

private fun deriveActiveQuickFilters(state: AppFilterState): Set<QuickFilter> = buildList {
    if (state.isSensitivePermissionsFilterActive) add(QuickFilter.SensitivePermissions)
    if (state.isLargeTotalFilterActive) add(QuickFilter.Large)
    if (state.isRecentInstallActive) add(QuickFilter.RecentlyInstalled)
    if (state.isRecentUpdateActive) add(QuickFilter.RecentlyUpdated)
}.toSet()
