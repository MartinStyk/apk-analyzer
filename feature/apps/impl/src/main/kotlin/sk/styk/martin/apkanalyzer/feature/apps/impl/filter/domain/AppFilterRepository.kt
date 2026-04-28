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
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFilterRepository @Inject constructor() {
    private val _filter = MutableStateFlow(AppFilterState())
    val filter: StateFlow<AppFilterState> = _filter.asStateFlow()

    val activeQuickFilters: Flow<ImmutableSet<QuickFilter>>
        get() = _filter.map { state -> deriveActiveQuickFilters(state) }

    fun update(filter: AppFilterState) {
        _filter.value = filter
    }

    fun clear() {
        _filter.value = AppFilterState()
    }

    fun toggleQuickFilter(filter: QuickFilter) {
        when (filter) {
            QuickFilter.LargeApps -> toggleLarge()
            QuickFilter.SystemApps -> toggleSystem()
            QuickFilter.Sideloaded -> toggleSideloaded()
            QuickFilter.RecentlyInstalled -> toggleRecentInstall()
            QuickFilter.RecentlyUpdated -> toggleRecentUpdate()
        }
    }

    private fun toggleLarge() {
        _filter.update { current ->
            if (current.isLargeFilterActive) {
                current.copy(apkSizeRange = null)
            } else {
                val currentMin = current.apkSizeRange?.min ?: 0.megabytes
                val min = maxOf(currentMin, AppFilterState.LARGE_APP_THRESHOLD)
                val max = current.apkSizeRange?.max ?: AppSize(Long.MAX_VALUE)
                current.copy(apkSizeRange = AppSizeRange(min = min, max = max))
            }
        }
    }

    private fun toggleSystem() {
        _filter.update { current ->
            current.copy(
                selectedSources = if (current.isSystemFilterActive) persistentSetOf() else persistentSetOf(AppSource.SystemPreinstalled),
            )
        }
    }

    private fun toggleSideloaded() {
        _filter.update { current ->
            current.copy(
                selectedSources = if (current.isSideloadedFilterActive) persistentSetOf() else persistentSetOf(AppSource.Unknown),
            )
        }
    }

    private fun toggleRecentInstall() {
        _filter.update { current ->
            val now = Instant.now()
            current.copy(
                installTimeRange = if (current.isRecentInstallActive) {
                    null
                } else {
                    DateRange(
                        start = now.minus(AppFilterState.TWO_MONTHS),
                        end = now,
                    )
                },
            )
        }
    }

    private fun toggleRecentUpdate() {
        _filter.update { current ->
            val now = Instant.now()
            current.copy(
                updateTimeRange = if (current.isRecentUpdateActive) {
                    null
                } else {
                    DateRange(
                        start = now.minus(AppFilterState.TWO_MONTHS),
                        end = now,
                    )
                },
            )
        }
    }

    private fun deriveActiveQuickFilters(state: AppFilterState): ImmutableSet<QuickFilter> = buildList {
        if (state.isLargeFilterActive) add(QuickFilter.LargeApps)
        if (state.isSystemFilterActive) add(QuickFilter.SystemApps)
        if (state.isSideloadedFilterActive) add(QuickFilter.Sideloaded)
        if (state.isRecentInstallActive) add(QuickFilter.RecentlyInstalled)
        if (state.isRecentUpdateActive) add(QuickFilter.RecentlyUpdated)
    }.toPersistentSet()
}
