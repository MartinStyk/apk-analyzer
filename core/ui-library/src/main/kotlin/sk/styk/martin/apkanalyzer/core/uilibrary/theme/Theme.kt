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

    CompositionLocalProvider(LocalApkAnalyzerColors provides apkAnalyzerColors) {
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

    val text: ApkAnalyzerTextStyles
        @Composable
        @ReadOnlyComposable
        get() {
            val typography = MaterialTheme.typography
            val c = colors
            return ApkAnalyzerTextStyles(
                displayLarge = typography.displayLarge.copy(color = c.textPrimary),
                displayMedium = typography.displayMedium.copy(color = c.textPrimary),
                displaySmall = typography.displaySmall.copy(color = c.textPrimary),
                headlineLarge = typography.headlineLarge.copy(color = c.textPrimary),
                headlineMedium = typography.headlineMedium.copy(color = c.textPrimary),
                headlineSmall = typography.headlineSmall.copy(color = c.textPrimary),
                titleLarge = typography.titleLarge.copy(color = c.textPrimary),
                titleMedium = typography.titleMedium.copy(color = c.textPrimary),
                titleSmall = typography.titleSmall.copy(color = c.textSecondary),
                bodyLarge = typography.bodyLarge.copy(color = c.textPrimary),
                bodyMedium = typography.bodyMedium.copy(color = c.textSecondary),
                bodySmall = typography.bodySmall.copy(color = c.textSecondary),
                labelLarge = typography.labelLarge.copy(color = c.textSecondary),
                labelMedium = typography.labelMedium.copy(color = c.textSecondary),
                labelSmall = typography.labelSmall.copy(color = c.textDisabled),
            )
        }
}
