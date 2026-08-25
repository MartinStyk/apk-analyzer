package sk.styk.martin.apkanalyzer.ui

import androidx.compose.runtime.Stable
import sk.styk.martin.apkanalyzer.core.common.settings.ColorAppScheme

@Stable
sealed interface ApkAnalyzerState {
    data object Loading : ApkAnalyzerState

    data class Data(val colorAppScheme: ColorAppScheme) : ApkAnalyzerState
}
