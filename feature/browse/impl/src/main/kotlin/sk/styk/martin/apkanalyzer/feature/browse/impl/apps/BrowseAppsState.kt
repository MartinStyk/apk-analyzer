package sk.styk.martin.apkanalyzer.feature.browse.impl.apps

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

@Immutable
internal sealed interface BrowseAppsState {

    data object Loading : BrowseAppsState

    @Immutable
    data class Loaded(
        val query: String,
        val totalApps: Int,
        val apps: List<BrowseAppItem>,
        val bucketDetail: BrowseBucketDetail? = null,
    ) : BrowseAppsState
}

@Immutable
internal data class BrowseAppItem(val packageName: PackageName, val applicationName: String)
