package sk.styk.martin.apkanalyzer.core.uilibrary.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ApkAnalyzerColorPalette(
    val background: Color,
    val onBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val accent: Color,
    val onAccent: Color,
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val neutral: Color,
    val chartPrimary: Color,
    val chartSecondary: Color,
    val chartTertiary: Color,
    val chartQuaternary: Color,
)

internal val LightApkAnalyzerColors =
    ApkAnalyzerColorPalette(
        background = Color(0xFFF8FAFA),
        onBackground = Color(0xFF101414),
        textPrimary = Color(0xFF101414),
        textSecondary = Color(0xFF3E4948),
        textDisabled = Color(0xFF707978),
        accent = Color(0xFF006766),
        onAccent = Color.White,
        positive = Color(0xFF2E7D32),
        negative = Color(0xFFC62828),
        warning = Color(0xFFE65100),
        neutral = Color(0xFF546E7A),
        chartPrimary = Color(0xFF006766),
        chartSecondary = Color(0xFF00BFA5),
        chartTertiary = Color(0xFF3E4948),
        chartQuaternary = Color(0xFF86D4D2),
    )

internal val DarkApkAnalyzerColors =
    ApkAnalyzerColorPalette(
        background = Color(0xFF101414),
        onBackground = Color(0xFFE0E3E2),
        textPrimary = Color(0xFFE0E3E2),
        textSecondary = Color(0xFFBFCBCB),
        textDisabled = Color(0xFF3E4948),
        accent = Color(0xFF86D4D2),
        onAccent = Color(0xFF003736),
        positive = Color(0xFF66BB6A),
        negative = Color(0xFFEF5350),
        warning = Color(0xFFFF9100),
        neutral = Color(0xFF90A4AE),
        chartPrimary = Color(0xFF86D4D2),
        chartSecondary = Color(0xFF7ADDD3),
        chartTertiary = Color(0xFFBFCBCB),
        chartQuaternary = Color(0xFF004F4E),
    )

internal fun lightColorScheme(colors: ApkAnalyzerColorPalette) = lightColorScheme(
    primary = colors.accent,
    onPrimary = colors.onAccent,
    primaryContainer = Color(0xFF86D4D2),
    onPrimaryContainer = Color(0xFF00201F),
    secondary = colors.textSecondary,
    onSecondary = colors.onAccent,
    secondaryContainer = Color(0xFFBFCBCB),
    onSecondaryContainer = Color(0xFF051F1E),
    tertiary = Color(0xFF00BFA5),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFA0EDE7),
    onTertiaryContainer = Color(0xFF003731),
    error = colors.negative,
    onError = colors.onAccent,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = colors.background,
    onBackground = colors.onBackground,
    surface = colors.background,
    onSurface = colors.onBackground,
    surfaceVariant = Color(0xFFE0E3E2),
    onSurfaceVariant = colors.textSecondary,
    inverseSurface = colors.onBackground,
    inverseOnSurface = colors.background,
    outline = colors.textDisabled,
)

internal fun darkColorScheme(colors: ApkAnalyzerColorPalette) = darkColorScheme(
    primary = colors.accent,
    onPrimary = colors.onAccent,
    primaryContainer = Color(0xFF004F4E),
    onPrimaryContainer = Color(0xFFB2EBEA),
    secondary = colors.textSecondary,
    onSecondary = colors.onAccent,
    secondaryContainer = Color(0xFF324B4A),
    onSecondaryContainer = Color(0xFFDBE4E3),
    tertiary = Color(0xFF7ADDD3),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF00504A),
    onTertiaryContainer = Color(0xFFA0EDE7),
    error = colors.negative,
    onError = colors.onAccent,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = colors.background,
    onBackground = colors.onBackground,
    surface = colors.background,
    onSurface = colors.onBackground,
    surfaceVariant = Color(0xFF181C1C),
    onSurfaceVariant = colors.textSecondary,
    inverseSurface = colors.onBackground,
    inverseOnSurface = colors.background,
    outline = colors.textDisabled,
)

internal val LocalApkAnalyzerColors = staticCompositionLocalOf { LightApkAnalyzerColors }
