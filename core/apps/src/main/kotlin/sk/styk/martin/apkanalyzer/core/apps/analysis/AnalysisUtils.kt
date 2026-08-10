package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PathPermission
import android.content.pm.PermissionInfo
import android.os.PatternMatcher
import sk.styk.martin.apkanalyzer.core.apps.model.AppCategory
import sk.styk.martin.apkanalyzer.core.apps.model.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledSplitApk
import sk.styk.martin.apkanalyzer.core.apps.model.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.model.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.model.ProviderPathMatchType
import sk.styk.martin.apkanalyzer.core.apps.model.ProviderPathPermission
import sk.styk.martin.apkanalyzer.core.apps.model.SplitApkKind
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.File
import java.util.zip.ZipFile

private const val TAG = "AnalysisUtils"

private val nativeLibraryEntryRegex = Regex("""^lib/([^/]+)/([^/]+\.so)$""")

private val STORE_INSTALLERS = mapOf(
    PackageName("com.android.vending") to AppSource.GooglePlay,
    PackageName("com.sec.android.app.samsungapps") to AppSource.SamsungGalaxyStore,
    PackageName("com.amazon.venezia") to AppSource.AmazonAppstore,
    PackageName("com.huawei.appmarket") to AppSource.HuaweiAppGallery,
    PackageName("com.xiaomi.market") to AppSource.XiaomiGetApps,
    PackageName("com.xiaomi.mipicks") to AppSource.XiaomiGetApps,
    PackageName("org.fdroid.fdroid") to AppSource.FDroid,
    PackageName("com.aurora.store") to AppSource.AuroraStore,
)

private val SIDELOAD_INSTALLERS = setOf(
    PackageName("com.google.android.packageinstaller"),
    PackageName("com.android.packageinstaller"),
    PackageName("com.android.chrome"),
    PackageName("org.mozilla.firefox"),
    PackageName("com.sec.android.app.sbrowser"),
    PackageName("com.microsoft.emmx"),
    PackageName("com.opera.browser"),
    PackageName("com.brave.browser"),
)

private val LOCAL_INSTALL_INSTALLERS = setOf(
    PackageName("pc"),
    PackageName("com.android.shell"),
)

private val SPLIT_ABI_QUALIFIERS = mapOf(
    "armeabi" to "armeabi",
    "armeabi_v7a" to "armeabi-v7a",
    "arm64_v8a" to "arm64-v8a",
    "x86" to "x86",
    "x86_64" to "x86_64",
    "mips" to "mips",
    "mips64" to "mips64",
)

private val SPLIT_DENSITY_QUALIFIERS = setOf(
    "ldpi", "mdpi", "tvdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "nodpi", "anydpi",
)

fun computeApkSize(applicationInfo: ApplicationInfo?): AppSize {
    val baseApkSize = applicationInfo?.sourceDir?.let { File(it).length() } ?: 0L
    val splitApksSize = readInstalledSplits(applicationInfo).sumOf { it.size.bytes }
    return (baseApkSize + splitApksSize).bytes
}

fun readInstalledSplits(applicationInfo: ApplicationInfo?): List<InstalledSplitApk> = applicationInfo?.splitSourceDirs.orEmpty().map { path ->
    val file = File(path)
    val (kind, qualifier) = classifySplitApk(file.name)
    InstalledSplitApk(
        fileName = file.name,
        filePath = path,
        size = file.length().bytes,
        kind = kind,
        qualifier = qualifier,
    )
}

private fun classifySplitApk(fileName: String): Pair<SplitApkKind, String> {
    val baseName = fileName.removeSuffix(".apk")
    val configQualifier = baseName.removePrefix("split_config.")
    if (configQualifier != baseName) {
        val abi = SPLIT_ABI_QUALIFIERS[configQualifier.lowercase()]
        return when {
            abi != null -> SplitApkKind.Abi to abi
            configQualifier.lowercase() in SPLIT_DENSITY_QUALIFIERS -> SplitApkKind.ScreenDensity to configQualifier
            else -> SplitApkKind.Language to configQualifier
        }
    }
    val moduleName = baseName.removePrefix("split_").substringBefore(".config.")
    return SplitApkKind.DynamicFeature to moduleName
}

fun isSystemInstalledApp(packageInfo: PackageInfo): Boolean = packageInfo.applicationInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false

fun resolveAppInstallSource(installSourceChain: InstallSourceChain, isSystemApp: Boolean): AppSource {
    val storeSource = installSourceChain.installingPackage?.let(STORE_INSTALLERS::get)
    return when {
        storeSource != null -> storeSource
        isSystemApp -> AppSource.SystemPreinstalled
        installSourceChain.matchesEither(LOCAL_INSTALL_INSTALLERS) -> AppSource.LocalInstall
        installSourceChain.matchesEither(SIDELOAD_INSTALLERS) -> AppSource.Sideloaded
        else -> AppSource.Unknown
    }
}

private fun InstallSourceChain.matchesEither(installers: Set<PackageName>): Boolean {
    val installing = installingPackage
    val initiating = initiatingPackage
    return (installing != null && installing in installers) || (initiating != null && initiating in installers)
}

fun readNativeLibraries(applicationInfo: ApplicationInfo?): NativeLibraries {
    val sourceDirs = listOfNotNull(applicationInfo?.sourceDir) + applicationInfo?.splitSourceDirs.orEmpty()
    val files = sourceDirs.flatMap { readNativeLibraryFiles(it) }.distinctBy { it.name to it.abi }
    return if (files.isEmpty()) NativeLibraries.Empty else NativeLibraries(files)
}

private fun readNativeLibraryFiles(sourceDir: String): List<NativeLibraryFile> = runCatching {
    val containingApkFileName = File(sourceDir).name
    ZipFile(sourceDir).use { zip ->
        zip.entries().asSequence()
            .mapNotNull { entry -> nativeLibraryEntryRegex.matchEntire(entry.name)?.let { it to entry } }
            .map { (match, entry) ->
                NativeLibraryFile(
                    name = match.groupValues[2],
                    abi = match.groupValues[1],
                    size = entry.size.coerceAtLeast(0).bytes,
                    containingApkFileName = containingApkFileName,
                )
            }
            .toList()
    }
}.getOrElse {
    Logger.w(TAG, it, "Can not read native libraries from $sourceDir")
    emptyList()
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
