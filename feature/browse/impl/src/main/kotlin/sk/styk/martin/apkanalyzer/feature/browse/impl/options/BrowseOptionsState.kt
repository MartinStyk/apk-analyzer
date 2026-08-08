package sk.styk.martin.apkanalyzer.feature.browse.impl.options

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface BrowseOptionsState {

    data object Loading : BrowseOptionsState

    @Immutable
    data class Loaded(
        val query: String,
        val subAttribute: String?,
        val totalOptions: Int,
        val options: ImmutableList<BrowseOption>,
    ) : BrowseOptionsState
}

@Immutable
internal data class BrowseOption(
    val key: String,
    val label: String,
    val rawIdentifier: String?,
    val count: Int,
)
