package sk.styk.martin.apkanalyzer.core.uilibrary.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

@Composable
fun ApkAnalyzerTheme(isDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val apkAnalyzerColors = if (isDarkTheme) DarkApkAnalyzerColors else LightApkAnalyzerColors
    val materialColorScheme =
        remember(isDarkTheme) {
            if (isDarkTheme) darkColorScheme(apkAnalyzerColors) else lightColorScheme(apkAnalyzerColors)
        }

    CompositionLocalProvider(
        LocalApkAnalyzerColors provides apkAnalyzerColors,
        LocalApkAnalyzerTypography provides ApkAnalyzerTypography.toApkAnalyzerTypographyStyles(),
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = ApkAnalyzerTypography,
            content = content,
        )
    }
}

object AppTheme {
    val colors: ApkAnalyzerColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalApkAnalyzerColors.current

    val typography: ApkAnalyzerTypographyStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalApkAnalyzerTypography.current
}
