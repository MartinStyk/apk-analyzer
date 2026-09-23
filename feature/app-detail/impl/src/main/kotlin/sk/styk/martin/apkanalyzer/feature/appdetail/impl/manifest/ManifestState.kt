package sk.styk.martin.apkanalyzer.feature.appdetail.impl.manifest

import androidx.compose.runtime.Immutable

@Immutable
internal data class ManifestLine(
    val number: Int,
    val text: String,
    val isMatch: Boolean,
)

@Immutable
internal sealed interface ManifestState {
    data object Loading : ManifestState
    data object Error : ManifestState

    @Immutable
    data class Loaded(
        val query: String,
        val displayedLines: List<ManifestLine>,
        val lineCount: Int,
        val matchCount: Int,
        val additionalInstalledSplits: Int,
    ) : ManifestState
}
