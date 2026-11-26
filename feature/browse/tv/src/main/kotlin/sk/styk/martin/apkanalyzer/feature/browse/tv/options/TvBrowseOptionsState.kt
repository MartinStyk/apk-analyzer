package sk.styk.martin.apkanalyzer.feature.browse.tv.options

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseBucketDetail
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseOption
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.feature.browse.tv.apps.TvBrowseAppItem

@Immutable
internal sealed interface TvBrowseOptionsState {

    data object Loading : TvBrowseOptionsState

    @Immutable
    data class Loaded(
        val query: String,
        val subAttribute: BrowseSubAttribute?,
        val subAttributes: List<BrowseSubAttribute>,
        val totalOptions: Int,
        val options: List<BrowseOption>,
        val preview: TvBrowseOptionPreview?,
    ) : TvBrowseOptionsState
}

@Immutable
internal data class TvBrowseOptionPreview(
    val option: BrowseOption,
    val detail: BrowseBucketDetail?,
    val apps: List<TvBrowseAppItem>,
    val totalApps: Int,
)
