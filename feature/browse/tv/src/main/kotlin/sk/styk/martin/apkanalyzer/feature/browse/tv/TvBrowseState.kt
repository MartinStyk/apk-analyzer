package sk.styk.martin.apkanalyzer.feature.browse.tv

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseOption
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

@Immutable
internal sealed interface TvBrowseState {

    data object Loading : TvBrowseState

    @Immutable
    data class Loaded(val totalApps: Int, val rows: List<TvBrowseRow>) : TvBrowseState
}

@Immutable
internal data class TvBrowseRow(
    val dimension: BrowseDimension,
    val subAttribute: BrowseSubAttribute?,
    val optionCount: Int,
    val options: List<TvBrowseRowOption>,
)

@Immutable
internal data class TvBrowseRowOption(val option: BrowseOption, val packages: List<PackageName>)
