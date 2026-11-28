package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.log

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryChangeCategory
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.access.LockableRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem
import java.time.YearMonth

@Immutable
internal sealed interface WhatChangedLogState {
    data object Loading : WhatChangedLogState

    data object Error : WhatChangedLogState

    data object Empty : WhatChangedLogState

    @Immutable
    data class Loaded(
        val months: List<AppChangeMonth>,
        val filters: List<LogFilter>,
        val selectedFilters: Set<LogFilter>,
    ) : WhatChangedLogState
}

@Immutable
internal data class AppChangeMonth(val month: YearMonth, val items: List<LockableRow<AppChangeItem>>)

@Immutable
internal sealed interface LogFilter {
    data object NeedsAttention : LogFilter

    data class Category(val category: AppHistoryChangeCategory) : LogFilter
}
