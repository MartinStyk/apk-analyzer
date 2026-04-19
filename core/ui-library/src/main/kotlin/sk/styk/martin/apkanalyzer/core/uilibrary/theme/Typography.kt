package sk.styk.martin.apkanalyzer.core.uilibrary.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import sk.styk.martin.apkanalyzer.core.uilibrary.R

private val provider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

private val ManropeFont = GoogleFont("Manrope")
private val InterFont = GoogleFont("Inter")

private val Manrope =
    FontFamily(
        Font(googleFont = ManropeFont, fontProvider = provider, weight = FontWeight.Normal),
        Font(googleFont = ManropeFont, fontProvider = provider, weight = FontWeight.Medium),
        Font(googleFont = ManropeFont, fontProvider = provider, weight = FontWeight.Bold),
    )

private val Inter =
    FontFamily(
        Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Normal),
        Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Medium),
        Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Bold),
    )

private val DisplayBase = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal)
private val BodyBase = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal)
private val LabelBase = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium)

internal val ApkAnalyzerTypography =
    Typography(
        displayLarge = DisplayBase.copy(fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
        displayMedium = DisplayBase.copy(fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
        displaySmall = DisplayBase.copy(fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
        headlineLarge = DisplayBase.copy(fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
        headlineMedium = DisplayBase.copy(fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
        headlineSmall = DisplayBase.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
        titleLarge = DisplayBase.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
        titleMedium = LabelBase.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        titleSmall = LabelBase.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge = BodyBase.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
        bodyMedium = BodyBase.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
        bodySmall = BodyBase.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        labelLarge = LabelBase.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = LabelBase.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
        labelSmall = LabelBase.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    )

@Immutable
data class ApkAnalyzerTextStyles(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)
