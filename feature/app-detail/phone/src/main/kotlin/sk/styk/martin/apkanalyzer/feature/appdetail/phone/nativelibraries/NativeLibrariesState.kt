package sk.styk.martin.apkanalyzer.feature.appdetail.phone.nativelibraries

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryItem
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.SectionErrorReason

@Immutable
internal sealed interface NativeLibrariesState {
    data object Loading : NativeLibrariesState

    data class Error(val reason: SectionErrorReason) : NativeLibrariesState

    data object NotRecorded : NativeLibrariesState

    @Immutable
    data class Loaded(
        val query: String,
        val totalCount: Int,
        val items: List<NativeLibraryItem>,
        val isHistorical: Boolean = false,
    ) : NativeLibrariesState {
        val hasResults: Boolean get() = items.isNotEmpty()
    }
}
