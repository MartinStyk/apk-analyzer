package sk.styk.martin.apkanalyzer.core.apps.filtering

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AppFilterRepository {
    val filter: StateFlow<AppFilterState>
    val activeQuickFilters: Flow<Set<QuickFilter>>
    val activeSourceQuickFilters: Flow<Set<SourceQuickFilter>>
    val activeActivityQuickFilter: Flow<ActivityQuickFilter?>
    fun update(filter: AppFilterState)
    fun clear()
    fun toggleQuickFilter(filter: QuickFilter)
    fun toggleSourceQuickFilter(filter: SourceQuickFilter)
    fun selectActivityQuickFilter(filter: ActivityQuickFilter?)
}
