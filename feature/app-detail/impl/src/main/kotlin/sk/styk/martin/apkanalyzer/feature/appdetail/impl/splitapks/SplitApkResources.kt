package sk.styk.martin.apkanalyzer.feature.appdetail.impl.splitapks

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import sk.styk.martin.apkanalyzer.core.apps.packaging.SplitApkKind
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import java.util.Locale

internal val SplitApkKind.icon: ImageVector
    get() = when (this) {
        SplitApkKind.DynamicFeature -> ApkAnalyzerIcons.Widgets
        SplitApkKind.Abi -> ApkAnalyzerIcons.Memory
        SplitApkKind.ScreenDensity -> ApkAnalyzerIcons.Screen
        SplitApkKind.Language -> ApkAnalyzerIcons.Language
    }

@get:StringRes
internal val SplitApkKind.labelRes: Int
    get() = when (this) {
        SplitApkKind.DynamicFeature -> R.string.splitapks_kind_feature
        SplitApkKind.Abi -> R.string.splitapks_kind_abi
        SplitApkKind.ScreenDensity -> R.string.splitapks_kind_density
        SplitApkKind.Language -> R.string.splitapks_kind_language
    }

@Composable
internal fun SplitApkKind.friendlyQualifier(qualifier: String): String = when (this) {
    SplitApkKind.Abi -> abiFriendlyName(qualifier) ?: qualifier
    SplitApkKind.ScreenDensity -> densityFriendlyName(qualifier) ?: qualifier
    SplitApkKind.Language -> languageFriendlyName(qualifier) ?: qualifier
    SplitApkKind.DynamicFeature -> qualifier
}

@Composable
private fun abiFriendlyName(qualifier: String): String? = when (qualifier) {
    "arm64-v8a" -> stringResource(R.string.splitapks_abi_arm64_v8a)
    "armeabi-v7a" -> stringResource(R.string.splitapks_abi_armeabi_v7a)
    "armeabi" -> stringResource(R.string.splitapks_abi_armeabi)
    "x86_64" -> stringResource(R.string.splitapks_abi_x86_64)
    "x86" -> stringResource(R.string.splitapks_abi_x86)
    "mips64" -> stringResource(R.string.splitapks_abi_mips64)
    "mips" -> stringResource(R.string.splitapks_abi_mips)
    else -> null
}

@Composable
private fun densityFriendlyName(qualifier: String): String? = when (qualifier) {
    "ldpi" -> stringResource(R.string.splitapks_density_ldpi)
    "mdpi" -> stringResource(R.string.splitapks_density_mdpi)
    "tvdpi" -> stringResource(R.string.splitapks_density_tvdpi)
    "hdpi" -> stringResource(R.string.splitapks_density_hdpi)
    "xhdpi" -> stringResource(R.string.splitapks_density_xhdpi)
    "xxhdpi" -> stringResource(R.string.splitapks_density_xxhdpi)
    "xxxhdpi" -> stringResource(R.string.splitapks_density_xxxhdpi)
    "nodpi" -> stringResource(R.string.splitapks_density_nodpi)
    "anydpi" -> stringResource(R.string.splitapks_density_anydpi)
    else -> null
}

private fun languageFriendlyName(qualifier: String): String? {
    val languageTag = qualifier.removePrefix("b+").replace('+', '-').replace("_r", "-").replace('_', '-')
    val locale = Locale.forLanguageTag(languageTag)
    val displayName = locale.getDisplayName(Locale.getDefault())
    return displayName.takeUnless { it.isBlank() || it.equals(languageTag, ignoreCase = true) }
}
