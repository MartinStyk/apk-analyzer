package sk.styk.martin.apkanalyzer.ui

import sk.styk.martin.apkanalyzer.core.common.settings.ColorAppScheme

sealed interface ApkAnalyzerState {
    data object Loading : ApkAnalyzerState

    data class Data(val colorAppScheme: ColorAppScheme) : ApkAnalyzerState
}
