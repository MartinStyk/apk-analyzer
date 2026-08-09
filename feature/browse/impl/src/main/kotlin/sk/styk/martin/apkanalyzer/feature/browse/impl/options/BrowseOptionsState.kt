package sk.styk.martin.apkanalyzer.feature.browse.impl.options

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.BrowseSubAttribute

@Immutable
internal sealed interface BrowseOptionsState {

    data object Loading : BrowseOptionsState

    @Immutable
    data class Loaded(
        val query: String,
        val subAttribute: BrowseSubAttribute?,
        val totalOptions: Int,
        val options: ImmutableList<BrowseOption>,
    ) : BrowseOptionsState
}

@Immutable
internal sealed interface BrowseOption {
    val key: String
    val label: String
    val count: Int

    @Immutable
    data class Labeled(
        override val key: String,
        override val label: String,
        val rawIdentifier: String?,
        override val count: Int,
    ) : BrowseOption

    @Immutable
    data class CertificateHash(
        override val key: String,
        override val label: String,
        val algorithm: BrowseSubAttribute,
        override val count: Int,
    ) : BrowseOption
}
