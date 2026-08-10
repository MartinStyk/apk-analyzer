package sk.styk.martin.apkanalyzer.feature.appdetail.impl.nativelibraries

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import sk.styk.martin.apkanalyzer.core.common.model.AppSize

@Immutable
internal sealed interface NativeLibrariesState {
    data object Loading : NativeLibrariesState

    data object Error : NativeLibrariesState

    @Immutable
    data class Loaded(
        val query: String,
        val totalCount: Int,
        val items: ImmutableList<NativeLibraryItem>,
    ) : NativeLibrariesState {
        val hasResults: Boolean get() = items.isNotEmpty()
    }
}

@Immutable
internal data class NativeLibraryItem(
    val name: String,
    val abis: ImmutableList<String>,
    val totalSize: AppSize,
    val isDeviceCompatible: Boolean,
    val variants: ImmutableList<NativeLibraryVariant>,
)

@Immutable
internal data class NativeLibraryVariant(
    val abi: String,
    val size: AppSize,
    val containingApkFileName: String,
)
