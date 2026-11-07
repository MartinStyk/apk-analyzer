package sk.styk.martin.apkanalyzer.feature.appdetail.phone.splitapks

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.packaging.InstalledSplitApk
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.RecordedVersionFailure

@Immutable
internal sealed interface SplitApksState {
    data object Loading : SplitApksState

    data object Error : SplitApksState

    @Immutable
    data class RecordedVersionError(val failure: RecordedVersionFailure) : SplitApksState

    @Immutable
    data class Loaded(
        val query: String,
        val totalCount: Int,
        val items: List<InstalledSplitApk>,
    ) : SplitApksState {
        val hasResults: Boolean get() = items.isNotEmpty()
    }
}
