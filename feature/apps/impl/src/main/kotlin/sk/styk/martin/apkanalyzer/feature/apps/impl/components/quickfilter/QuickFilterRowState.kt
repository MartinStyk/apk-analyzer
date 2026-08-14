package sk.styk.martin.apkanalyzer.feature.apps.impl.components.quickfilter

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import sk.styk.martin.apkanalyzer.feature.apps.impl.components.AppDataPermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.ActivityQuickFilter
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.QuickFilter
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.SourceQuickFilter

@Immutable
data class QuickFilterRowState(
    val activeQuickFilters: ImmutableSet<QuickFilter> = persistentSetOf(),
    val activeSourceQuickFilters: ImmutableSet<SourceQuickFilter> = persistentSetOf(),
    val activeActivityQuickFilter: ActivityQuickFilter? = null,
    val isDeepFilterActive: Boolean = false,
    val permissionRationale: AppDataPermission? = null,
)
