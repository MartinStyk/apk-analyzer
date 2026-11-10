package sk.styk.martin.apkanalyzer.feature.browse.tv.apps

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseBucketDetail
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

@Immutable
internal sealed interface TvBrowseAppsState {

    data object Loading : TvBrowseAppsState

    @Immutable
    data class Loaded(
        val query: String,
        val totalApps: Int,
        val apps: List<TvBrowseAppItem>,
        val bucketDetail: BrowseBucketDetail?,
    ) : TvBrowseAppsState
}

@Immutable
internal data class TvBrowseAppItem(val packageName: PackageName, val applicationName: String)
