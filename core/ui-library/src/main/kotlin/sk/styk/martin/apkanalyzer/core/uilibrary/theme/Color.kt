@file:Suppress("MatchingDeclarationName")

package sk.styk.martin.apkanalyzer.core.uilibrary.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ApkAnalyzerColorPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,

    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,

    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    val positive: Color,
    val positiveContainer: Color,
    val negative: Color,
    val negativeContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val aiAccent: Color,
    val aiAccentContainer: Color,

    val shadow: Color,
    val pillBorder: Color,
)

internal val LightApkAnalyzerColors = ApkAnalyzerColorPalette(
    background = Color(0xFFf7f7f7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E5EA),
    onBackground = Color(0xFF191C1C),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF717173),
    primary = Color(0xFF006766),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2EBEA),
    secondary = Color(0xFF607C7B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6F0EF),
    onSecondaryContainer = Color(0xFF1C3534),
    tertiary = Color(0xFF894C2D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBC9),
    onTertiaryContainer = Color(0xFF3B1600),
    positive = Color(0xFF34C759),
    positiveContainer = Color(0xFFE8F5E9),
    negative = Color(0xFFFF3B30),
    negativeContainer = Color(0xFFFFEBEE),
    warning = Color(0xFFFF9500),
    warningContainer = Color(0xFFFFF3E0),
    aiAccent = Color(0xFF7C4DFF),
    aiAccentContainer = Color(0xFFEDE7FE),
    shadow = Color(0x1A000000),
    pillBorder = Color.Transparent,
)

internal val DarkApkAnalyzerColors = ApkAnalyzerColorPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF161618),
    surfaceVariant = Color(0xFF1d1d1d),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF8E8E93),
    primary = Color(0xFF86D4D2),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1A3F3E),
    secondary = Color(0xFF607C7B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2A4A49),
    onSecondaryContainer = Color(0xFFD6F0EF),
    tertiary = Color(0xFF607C7B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF4A2A14),
    onTertiaryContainer = Color(0xFFFFDBC9),
    positive = Color(0xFF34C759),
    positiveContainer = Color(0xFF1B3A1C),
    negative = Color(0xFFFF453A),
    negativeContainer = Color(0xFF3E1414),
    warning = Color(0xFFFF9F0A),
    warningContainer = Color(0xFF3E2A0F),
    aiAccent = Color(0xFFB39DFF),
    aiAccentContainer = Color(0xFF2E2251),
    shadow = Color(0x00000000),
    pillBorder = Color(0xFF2C2C2E),
)

internal fun lightColorScheme(colors: ApkAnalyzerColorPalette) = lightColorScheme(
    primary = colors.primary,
    onPrimary = colors.onPrimary,
    primaryContainer = colors.primaryContainer,
    onPrimaryContainer = colors.primary,
    secondary = colors.secondary,
    onSecondary = colors.onSecondary,
    secondaryContainer = colors.secondaryContainer,
    onSecondaryContainer = colors.onSecondaryContainer,
    tertiary = colors.tertiary,
    onTertiary = colors.onTertiary,
    tertiaryContainer = colors.tertiaryContainer,
    onTertiaryContainer = colors.onTertiaryContainer,
    background = colors.background,
    onBackground = colors.onBackground,
    surface = colors.surface,
    onSurface = colors.onSurface,
    surfaceVariant = colors.surfaceVariant,
    onSurfaceVariant = colors.onSurfaceVariant,
    error = colors.negative,
    onError = colors.surface,
    errorContainer = colors.negativeContainer,
    onErrorContainer = colors.negative,
    outline = colors.onSurfaceVariant,
)

internal fun darkColorScheme(colors: ApkAnalyzerColorPalette) = darkColorScheme(
    primary = colors.primary,
    onPrimary = colors.onPrimary,
    primaryContainer = colors.primaryContainer,
    onPrimaryContainer = colors.primary,
    secondary = colors.secondary,
    onSecondary = colors.onSecondary,
    secondaryContainer = colors.secondaryContainer,
    onSecondaryContainer = colors.onSecondaryContainer,
    tertiary = colors.tertiary,
    onTertiary = colors.onTertiary,
    tertiaryContainer = colors.tertiaryContainer,
    onTertiaryContainer = colors.onTertiaryContainer,
    background = colors.background,
    onBackground = colors.onBackground,
    surface = colors.surface,
    onSurface = colors.onSurface,
    surfaceVariant = colors.surfaceVariant,
    onSurfaceVariant = colors.onSurfaceVariant,
    error = colors.negative,
    onError = colors.surface,
    errorContainer = colors.negativeContainer,
    onErrorContainer = colors.negative,
    outline = colors.onSurfaceVariant,
)

internal val LocalApkAnalyzerColors = staticCompositionLocalOf { LightApkAnalyzerColors }
