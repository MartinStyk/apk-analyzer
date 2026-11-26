package sk.styk.martin.apkanalyzer.core.apps.packaging

import android.content.res.Resources
import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.apps.R
import java.util.Locale

@get:StringRes
val SplitApkKind.labelRes: Int
    get() = when (this) {
        SplitApkKind.DynamicFeature -> R.string.splitapks_kind_feature
        SplitApkKind.Abi -> R.string.splitapks_kind_abi
        SplitApkKind.ScreenDensity -> R.string.splitapks_kind_density
        SplitApkKind.Language -> R.string.splitapks_kind_language
    }

fun SplitApkKind.friendlyQualifier(qualifier: String, resources: Resources): String = when (this) {
    SplitApkKind.Abi -> abiNames[qualifier]?.let(resources::getString) ?: qualifier
    SplitApkKind.ScreenDensity -> densityNames[qualifier]?.let(resources::getString) ?: qualifier
    SplitApkKind.Language -> languageFriendlyName(qualifier) ?: qualifier
    SplitApkKind.DynamicFeature -> qualifier
}

private val abiNames = mapOf(
    "arm64-v8a" to R.string.splitapks_abi_arm64_v8a,
    "armeabi-v7a" to R.string.splitapks_abi_armeabi_v7a,
    "armeabi" to R.string.splitapks_abi_armeabi,
    "x86_64" to R.string.splitapks_abi_x86_64,
    "x86" to R.string.splitapks_abi_x86,
    "mips64" to R.string.splitapks_abi_mips64,
    "mips" to R.string.splitapks_abi_mips,
)

private val densityNames = mapOf(
    "ldpi" to R.string.splitapks_density_ldpi,
    "mdpi" to R.string.splitapks_density_mdpi,
    "tvdpi" to R.string.splitapks_density_tvdpi,
    "hdpi" to R.string.splitapks_density_hdpi,
    "xhdpi" to R.string.splitapks_density_xhdpi,
    "xxhdpi" to R.string.splitapks_density_xxhdpi,
    "xxxhdpi" to R.string.splitapks_density_xxxhdpi,
    "nodpi" to R.string.splitapks_density_nodpi,
    "anydpi" to R.string.splitapks_density_anydpi,
)

private fun languageFriendlyName(qualifier: String): String? {
    val languageTag = qualifier.removePrefix("b+").replace('+', '-').replace("_r", "-").replace('_', '-')
    val locale = Locale.forLanguageTag(languageTag)
    val displayName = locale.getDisplayName(Locale.getDefault())
    return displayName.takeUnless { it.isBlank() || it.equals(languageTag, ignoreCase = true) }
}
