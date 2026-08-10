package sk.styk.martin.apkanalyzer.core.apps.packaging

import android.content.pm.ApplicationInfo
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.File
import java.util.zip.ZipFile

private const val TAG = "ApkPackagingAnalysis"

private val nativeLibraryEntryRegex = Regex("""^lib/([^/]+)/([^/]+\.so)$""")

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
