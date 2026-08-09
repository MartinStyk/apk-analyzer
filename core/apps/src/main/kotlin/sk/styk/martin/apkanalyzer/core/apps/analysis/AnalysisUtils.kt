package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PathPermission
import android.content.pm.PermissionInfo
import android.os.PatternMatcher
import sk.styk.martin.apkanalyzer.core.apps.model.AppCategory
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.model.ProviderPathMatchType
import sk.styk.martin.apkanalyzer.core.apps.model.ProviderPathPermission
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.File

private val GOOGLE_PLAY_INSTALLER = PackageName("com.android.vending")

fun computeApkSize(applicationInfo: ApplicationInfo?): AppSize {
    val baseApkSize = applicationInfo?.sourceDir?.let { File(it).length() } ?: 0L
    val splitApksSize = applicationInfo?.splitSourceDirs.orEmpty().sumOf { File(it).length() }
    return (baseApkSize + splitApksSize).bytes
}

internal fun isSystemInstalledApp(packageInfo: PackageInfo): Boolean =
    packageInfo.applicationInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false

internal fun resolveAppInstallSource(installingPackage: PackageName?, isSystemApp: Boolean): AppSource = when {
    installingPackage == GOOGLE_PLAY_INSTALLER -> AppSource.GooglePlay
    isSystemApp -> AppSource.SystemPreinstalled
    else -> AppSource.Unknown
}

@Suppress("DEPRECATION")
internal fun resolveProtectionLevel(protection: Int): ProtectionLevel = when (protection) {
    PermissionInfo.PROTECTION_DANGEROUS -> ProtectionLevel.Dangerous
    PermissionInfo.PROTECTION_SIGNATURE, PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> ProtectionLevel.Signature
    PermissionInfo.PROTECTION_INTERNAL -> ProtectionLevel.Internal
    else -> ProtectionLevel.Normal
}

internal fun resolveProtectionFlags(protectionFlags: Int): Set<ProtectionFlag> = protectionFlagsByMask
    .filterKeys { protectionFlags and it != 0 }
    .values
    .toSet()

private val protectionFlagsByMask = mapOf(
    PermissionInfo.PROTECTION_FLAG_PRIVILEGED to ProtectionFlag.Privileged,
    PermissionInfo.PROTECTION_FLAG_APPOP to ProtectionFlag.AppOp,
    PermissionInfo.PROTECTION_FLAG_INSTANT to ProtectionFlag.Instant,
    PermissionInfo.PROTECTION_FLAG_DEVELOPMENT to ProtectionFlag.Development,
)

internal fun resolvePathPermissions(pathPermissions: Array<PathPermission>?): List<ProviderPathPermission> = pathPermissions.orEmpty().map {
    ProviderPathPermission(
        path = it.path,
        matchType = resolvePathMatchType(it.type),
        readPermission = it.readPermission,
        writePermission = it.writePermission,
    )
}

private fun resolvePathMatchType(type: Int): ProviderPathMatchType = when (type) {
    PatternMatcher.PATTERN_LITERAL -> ProviderPathMatchType.Literal
    PatternMatcher.PATTERN_PREFIX -> ProviderPathMatchType.Prefix
    PatternMatcher.PATTERN_SIMPLE_GLOB -> ProviderPathMatchType.SimpleGlob
    PatternMatcher.PATTERN_ADVANCED_GLOB -> ProviderPathMatchType.AdvancedGlob
    PatternMatcher.PATTERN_SUFFIX -> ProviderPathMatchType.Suffix
    else -> ProviderPathMatchType.Literal
}

internal fun resolveAppCategory(category: Int): AppCategory = when (category) {
    ApplicationInfo.CATEGORY_GAME -> AppCategory.Game
    ApplicationInfo.CATEGORY_AUDIO -> AppCategory.Audio
    ApplicationInfo.CATEGORY_VIDEO -> AppCategory.Video
    ApplicationInfo.CATEGORY_IMAGE -> AppCategory.Image
    ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.Social
    ApplicationInfo.CATEGORY_NEWS -> AppCategory.News
    ApplicationInfo.CATEGORY_MAPS -> AppCategory.Maps
    ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.Productivity
    ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.Accessibility
    else -> AppCategory.Undefined
}
