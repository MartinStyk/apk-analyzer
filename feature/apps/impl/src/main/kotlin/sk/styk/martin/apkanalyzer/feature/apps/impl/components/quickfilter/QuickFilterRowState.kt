package sk.styk.martin.apkanalyzer.feature.apps.impl.components.quickfilter

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import sk.styk.martin.apkanalyzer.feature.apps.impl.components.AppDataPermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.QuickFilter

@Immutable
data class QuickFilterRowState(
    val activeQuickFilters: ImmutableSet<QuickFilter> = persistentSetOf(),
    val isDeepFilterActive: Boolean = false,
    val permissionRationale: AppDataPermission? = null,
)
