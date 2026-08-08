package sk.styk.martin.apkanalyzer.ui.externalapk

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

@Immutable
internal sealed interface ExternalApkState {
    data object Loading : ExternalApkState

    data class Loaded(val input: AppDetailInput.ApkFile) : ExternalApkState

    data object Error : ExternalApkState
}
