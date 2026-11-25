package sk.styk.martin.apkanalyzer.feature.appdetail.tv.nativelibraries

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryItem

@Immutable
internal sealed interface TvNativeLibrariesState {
    data object Loading : TvNativeLibrariesState

    data object Error : TvNativeLibrariesState

    @Immutable
    data class Loaded(val items: List<NativeLibraryItem>, val deviceAbis: List<String>) : TvNativeLibrariesState
}
