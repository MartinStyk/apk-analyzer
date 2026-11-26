package sk.styk.martin.apkanalyzer.feature.browse.phone

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimensionSummary

@Immutable
internal sealed interface BrowseState {

    data object Loading : BrowseState

    @Immutable
    data class Loaded(val totalApps: Int, val dimensions: List<BrowseDimensionSummary>) : BrowseState
}
