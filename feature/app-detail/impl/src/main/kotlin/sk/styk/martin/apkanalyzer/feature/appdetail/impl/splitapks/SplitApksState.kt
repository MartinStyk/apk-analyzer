package sk.styk.martin.apkanalyzer.feature.appdetail.impl.splitapks

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledSplitApk

@Immutable
internal sealed interface SplitApksState {
    data object Loading : SplitApksState

    data object Error : SplitApksState

    @Immutable
    data class Loaded(
        val query: String,
        val totalCount: Int,
        val items: ImmutableList<InstalledSplitApk>,
    ) : SplitApksState {
        val hasResults: Boolean get() = items.isNotEmpty()
    }
}
