package sk.styk.martin.apkanalyzer.ui

import androidx.compose.runtime.Stable
import sk.styk.martin.apkanalyzer.core.common.settings.ColorAppScheme
import sk.styk.martin.apkanalyzer.core.common.settings.FingerprintFormat

@Stable
sealed interface ApkAnalyzerState {
    data object Loading : ApkAnalyzerState

    data class Data(val colorAppScheme: ColorAppScheme, val fingerprintFormat: FingerprintFormat) : ApkAnalyzerState
}
