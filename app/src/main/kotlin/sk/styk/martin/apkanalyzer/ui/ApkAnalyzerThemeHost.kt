package sk.styk.martin.apkanalyzer.ui

import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
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
    SyncSystemBarsWithTheme(isDarkTheme = isDarkTheme)
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

@Composable
private fun SyncSystemBarsWithTheme(isDarkTheme: Boolean) {
    val view = LocalView.current
    val activity = LocalActivity.current
    if (view.isInEditMode || activity == null) return

    DisposableEffect(activity, isDarkTheme) {
        val insetsController = WindowCompat.getInsetsController(activity.window, view)
        insetsController.isAppearanceLightStatusBars = !isDarkTheme
        insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        onDispose {}
    }
}

@Preview
@Composable
private fun ApkAnalyzerThemeHostPreview() {
    ApkAnalyzerThemeHost(state = ApkAnalyzerState.Loading) {}
}
