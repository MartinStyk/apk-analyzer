package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.apps.AppClassificationThresholds
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import sk.styk.martin.apkanalyzer.core.common.model.isSideloaded
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFilterRepository @Inject constructor() {
    private val _filter = MutableStateFlow(AppFilterState())
    val filter: StateFlow<AppFilterState> = _filter.asStateFlow()

    val activeQuickFilters: Flow<ImmutableSet<QuickFilter>>
        get() = _filter.map { deriveActiveQuickFilters(it) }

    val activeSourceQuickFilters: Flow<ImmutableSet<SourceQuickFilter>>
        get() = _filter.map { it.activeSourceQuickFilters }

    val activeActivityQuickFilter: Flow<ActivityQuickFilter?>
        get() = _filter.map { it.activeActivityQuickFilter }

    fun update(filter: AppFilterState) {
        _filter.value = filter
    }

    fun clear() {
        _filter.value = AppFilterState()
    }

    fun toggleQuickFilter(filter: QuickFilter) {
        _filter.update { current ->
            when (filter) {
                QuickFilter.SensitivePermissions -> toggleSensitivePermissions(current)
                QuickFilter.Large -> toggleLargeTotal(current)
                QuickFilter.RecentlyInstalled -> toggleRecentInstall(current)
                QuickFilter.RecentlyUpdated -> toggleRecentUpdate(current)
            }
        }
    }

    fun toggleSourceQuickFilter(filter: SourceQuickFilter) {
        _filter.update { current ->
            val sources = filter.appSources()
            val allSelected = sources.all { it in current.selectedSources }
            current.copy(
                selectedSources = if (allSelected) {
                    (current.selectedSources - sources).toPersistentSet()
                } else {
                    (current.selectedSources + sources).toPersistentSet()
                },
            )
        }
    }

    private fun SourceQuickFilter.appSources(): Set<AppSource> = when (this) {
        SourceQuickFilter.System -> setOf(AppSource.SystemPreinstalled)
        SourceQuickFilter.GooglePlay -> setOf(AppSource.GooglePlay)
        SourceQuickFilter.Sideloaded -> AppSource.entries.filter { it.isSideloaded }.toSet()
    }

    fun selectActivityQuickFilter(filter: ActivityQuickFilter?) {
        _filter.update { current ->
            current.copy(
                recentlyUsedDays = if (filter == ActivityQuickFilter.RecentlyUsed) AppClassificationThresholds.RECENTLY_USED_DAYS else null,
                unusedPeriod = if (filter == ActivityQuickFilter.Unused) UnusedAppsPeriod.SIX_MONTHS else null,
            )
        }
    }

    private fun toggleSensitivePermissions(current: AppFilterState): AppFilterState = current.copy(
        selectedPermissions = if (current.isSensitivePermissionsFilterActive) {
            persistentSetOf()
        } else {
            PermissionPreset.Sensitive.permissions.toPersistentSet()
        },
        permissionMatchAll = false,
    )

    private fun toggleLargeTotal(current: AppFilterState): AppFilterState = if (current.isLargeTotalFilterActive) {
        current.copy(totalSizeRange = null)
    } else {
        val min = AppClassificationThresholds.LARGE_SIZE
        val max = current.totalSizeRange?.max ?: Long.MAX_VALUE.bytes
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

private fun deriveActiveQuickFilters(state: AppFilterState): ImmutableSet<QuickFilter> = buildList {
    if (state.isSensitivePermissionsFilterActive) add(QuickFilter.SensitivePermissions)
    if (state.isLargeTotalFilterActive) add(QuickFilter.Large)
    if (state.isRecentInstallActive) add(QuickFilter.RecentlyInstalled)
    if (state.isRecentUpdateActive) add(QuickFilter.RecentlyUpdated)
}.toPersistentSet()
