package sk.styk.martin.apkanalyzer.feature.appdetail.tv.splitapks

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.packaging.InstalledSplitApk

@Immutable
internal sealed interface TvSplitApksState {
    data object Loading : TvSplitApksState

    data object Error : TvSplitApksState

    @Immutable
    data class Loaded(val splits: List<InstalledSplitApk>) : TvSplitApksState
}
