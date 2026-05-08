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
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFilterRepository @Inject constructor() {
    private val _filter = MutableStateFlow(AppFilterState())
    val filter: StateFlow<AppFilterState> = _filter.asStateFlow()

    val activeQuickFilters: Flow<ImmutableSet<QuickFilter>>
        get() = _filter.map { deriveActiveQuickFilters(it) }

    fun update(filter: AppFilterState) {
        _filter.value = filter
    }

    fun clear() {
        _filter.value = AppFilterState()
    }

    fun toggleQuickFilter(filter: QuickFilter) {
        _filter.update { current ->
            when (filter) {
                QuickFilter.Large -> toggleLargeTotal(current)
                QuickFilter.RecentlyUsed -> toggleRecentlyUsed(current)
                QuickFilter.Unused -> toggleUnused(current)
                QuickFilter.System -> toggleSource(current, AppSource.SystemPreinstalled)
                QuickFilter.GooglePlay -> toggleSource(current, AppSource.GooglePlay)
                QuickFilter.Sideloaded -> toggleSource(current, AppSource.Unknown)
                QuickFilter.RecentlyInstalled -> toggleRecentInstall(current)
                QuickFilter.RecentlyUpdated -> toggleRecentUpdate(current)
            }
        }
    }

    private fun toggleSource(current: AppFilterState, source: AppSource): AppFilterState = current.copy(
        selectedSources = if (source in current.selectedSources) persistentSetOf() else persistentSetOf(source),
    )

    private fun toggleLargeTotal(current: AppFilterState): AppFilterState = if (current.isLargeTotalFilterActive) {
        current.copy(totalSizeRange = null)
    } else {
        val min = AppFilterState.LARGE_TOTAL_SIZE_THRESHOLD
        val max = current.totalSizeRange?.max ?: AppSize(Long.MAX_VALUE)
        current.copy(totalSizeRange = AppSizeRange(min = min, max = max))
    }

    private fun toggleRecentlyUsed(current: AppFilterState): AppFilterState = current.copy(
        recentlyUsedDays = if (current.isRecentlyUsedActive) null else RECENTLY_USED_DAYS,
        unusedPeriod = null,
    )

    private fun toggleUnused(current: AppFilterState): AppFilterState = current.copy(
        unusedPeriod = if (current.isUnusedFilterActive) null else UnusedAppsPeriod.SIX_MONTHS,
        recentlyUsedDays = null,
    )

    private fun toggleRecentInstall(current: AppFilterState): AppFilterState {
        val now = Instant.now()
        return current.copy(
            installTimeRange = if (current.isRecentInstallActive) {
                null
            } else {
                DateRange(start = now.minus(AppFilterState.ONE_MONTH), end = now)
            },
        )
    }

    private fun toggleRecentUpdate(current: AppFilterState): AppFilterState {
        val now = Instant.now()
        return current.copy(
            updateTimeRange = if (current.isRecentUpdateActive) {
                null
            } else {
                DateRange(start = now.minus(AppFilterState.ONE_MONTH), end = now)
            },
        )
    }

    private fun deriveActiveQuickFilters(state: AppFilterState): ImmutableSet<QuickFilter> = buildList {
        if (state.isLargeTotalFilterActive) add(QuickFilter.Large)
        if (state.isRecentlyUsedActive) add(QuickFilter.RecentlyUsed)
        if (state.isUnusedFilterActive) add(QuickFilter.Unused)
        if (state.isSystemFilterActive) add(QuickFilter.System)
        if (state.isGooglePlayFilterActive) add(QuickFilter.GooglePlay)
        if (state.isSideloadedFilterActive) add(QuickFilter.Sideloaded)
        if (state.isRecentInstallActive) add(QuickFilter.RecentlyInstalled)
        if (state.isRecentUpdateActive) add(QuickFilter.RecentlyUpdated)
    }.toPersistentSet()

    private companion object {
        const val RECENTLY_USED_DAYS = 2
    }
}
