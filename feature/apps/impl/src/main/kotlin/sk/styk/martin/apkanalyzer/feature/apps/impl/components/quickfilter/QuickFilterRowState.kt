package sk.styk.martin.apkanalyzer.feature.apps.impl.components.quickfilter

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.feature.apps.impl.components.AppDataPermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.ActivityQuickFilter
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.QuickFilter
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.SourceQuickFilter

@Immutable
data class QuickFilterRowState(
    val activeQuickFilters: Set<QuickFilter> = setOf(),
    val activeSourceQuickFilters: Set<SourceQuickFilter> = setOf(),
    val activeActivityQuickFilter: ActivityQuickFilter? = null,
    val isDeepFilterActive: Boolean = false,
    val permissionRationale: AppDataPermission? = null,
)
