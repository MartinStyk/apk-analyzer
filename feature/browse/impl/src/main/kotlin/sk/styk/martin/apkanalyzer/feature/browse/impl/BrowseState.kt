package sk.styk.martin.apkanalyzer.feature.browse.impl

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

@Immutable
internal sealed interface BrowseState {

    data object Loading : BrowseState

    @Immutable
    data class Loaded(val totalApps: Int, val dimensions: ImmutableList<DimensionSummary>) : BrowseState
}

@Immutable
internal data class DimensionSummary(
    val dimension: BrowseDimension,
    val optionCount: Int,
    val topLabels: ImmutableList<String>,
)
