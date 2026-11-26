package sk.styk.martin.apkanalyzer.feature.apps.phone.components.quickfilter

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.ActivityQuickFilter
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.QuickFilter
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.SourceQuickFilter
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.AppDataPermission

@Immutable
data class QuickFilterRowState(
    val activeQuickFilters: Set<QuickFilter> = setOf(),
    val activeSourceQuickFilters: Set<SourceQuickFilter> = setOf(),
    val activeActivityQuickFilter: ActivityQuickFilter? = null,
    val isDeepFilterActive: Boolean = false,
    val permissionRationale: AppDataPermission? = null,
)
