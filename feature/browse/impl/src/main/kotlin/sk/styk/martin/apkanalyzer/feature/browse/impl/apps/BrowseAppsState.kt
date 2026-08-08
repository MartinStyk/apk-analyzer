package sk.styk.martin.apkanalyzer.feature.browse.impl.apps

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

@Immutable
internal sealed interface BrowseAppsState {

    data object Loading : BrowseAppsState

    @Immutable
    data class Loaded(
        val query: String,
        val totalApps: Int,
        val apps: ImmutableList<BrowseAppItem>,
    ) : BrowseAppsState
}

@Immutable
internal data class BrowseAppItem(val packageName: PackageName, val applicationName: String)
