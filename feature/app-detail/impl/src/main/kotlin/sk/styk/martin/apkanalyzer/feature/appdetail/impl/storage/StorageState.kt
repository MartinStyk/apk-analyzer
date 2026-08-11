package sk.styk.martin.apkanalyzer.feature.appdetail.impl.storage

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageBreakdown

@Immutable
internal sealed interface StorageState {
    data object Loading : StorageState

    data object Error : StorageState

    data object MissingPermission : StorageState

    @Immutable
    data class Loaded(val breakdown: StorageBreakdown) : StorageState
}
