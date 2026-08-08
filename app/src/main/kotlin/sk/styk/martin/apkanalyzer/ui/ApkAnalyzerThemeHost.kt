package sk.styk.martin.apkanalyzer.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.common.settings.ColorAppScheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme

@Composable
internal fun ApkAnalyzerThemeHost(state: ApkAnalyzerState, content: @Composable () -> Unit) {
    val isDarkTheme = when (state) {
        ApkAnalyzerState.Loading -> isSystemInDarkTheme()

        is ApkAnalyzerState.Data -> when (state.colorAppScheme) {
            ColorAppScheme.Day -> false
            ColorAppScheme.Night -> true
            ColorAppScheme.FollowSystem -> isSystemInDarkTheme()
        }
    }
    val nightMode = when (state) {
        ApkAnalyzerState.Loading -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        is ApkAnalyzerState.Data -> when (state.colorAppScheme) {
            ColorAppScheme.Day -> AppCompatDelegate.MODE_NIGHT_NO
            ColorAppScheme.Night -> AppCompatDelegate.MODE_NIGHT_YES
            ColorAppScheme.FollowSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
    LaunchedEffect(nightMode) {
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
    ApkAnalyzerTheme(isDarkTheme = isDarkTheme, content = content)
}

@Preview
@Composable
private fun ApkAnalyzerThemeHostPreview() {
    ApkAnalyzerThemeHost(state = ApkAnalyzerState.Loading) {}
}
