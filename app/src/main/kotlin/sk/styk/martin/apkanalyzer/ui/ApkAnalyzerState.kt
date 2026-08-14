package sk.styk.martin.apkanalyzer.ui

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.common.settings.ColorAppScheme

@Immutable
sealed interface ApkAnalyzerState {
    data object Loading : ApkAnalyzerState

    data class Data(val colorAppScheme: ColorAppScheme) : ApkAnalyzerState
}
