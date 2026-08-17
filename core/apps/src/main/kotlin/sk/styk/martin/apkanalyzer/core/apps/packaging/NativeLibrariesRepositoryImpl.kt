package sk.styk.martin.apkanalyzer.core.apps.packaging

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.AppReferenceCacheKey
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import sk.styk.martin.apkanalyzer.core.common.model.toCacheKey
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrace
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.analysisMode
import sk.styk.martin.apkanalyzer.core.common.performance.analysisModeAttribute
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NativeLibrariesRepositoryImpl"
private const val CACHE_HIT_ATTRIBUTE = "cache_hit"
private const val NATIVE_LIBRARY_ENTRY_PREFIX = "lib/"
private val nativeLibraryEntryRegex = Regex("""^lib/([^/]+)/([^/]+\.so)$""")

@Singleton
internal class NativeLibrariesRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    packageChangesObserver: PackageChangesObserver,
    appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val performanceTracker: PerformanceTracker,
) : NativeLibrariesRepository {

    private val cache = ConcurrentHashMap<AppReferenceCacheKey, NativeLibraries>()

    init {
        packageChangesObserver.observe()
            .onEach {
                Logger.d(TAG, "Package change detected, clearing cache")
                cache.clear()
            }
            .launchIn(appScope + dispatcherProvider.default())
    }

    override suspend fun nativeLibraries(reference: AppReference): Result<NativeLibraries> = performanceTracker.startCancellableTrace("native_libraries_load") {
        analysisMode = reference.analysisModeAttribute
        val cacheKey = reference.toCacheKey()
        cache[cacheKey]?.let {
            markCacheHit()
            outcome = TraceOutcome.Success
            return@startCancellableTrace Result.success(it)
        }
        runCatchingCancellable {
            val applicationInfo = resolveApplicationInfo(reference)
            readNativeLibraries(applicationInfo, dispatcherProvider.io())
        }.onSuccess {
            cache[cacheKey] = it
            outcome = TraceOutcome.Success
        }.onFailure {
            outcome = TraceOutcome.Error
        }
    }

    private fun resolveApplicationInfo(reference: AppReference): ApplicationInfo = when (reference) {
        is AppReference.InstalledPackage -> packageManager.getApplicationInfo(reference.packageName.value, 0)

        is AppReference.ApkFile -> packageManager.getPackageArchiveInfo(reference.path, 0)
            ?.applicationInfo
            ?.apply {
                sourceDir = reference.path
                publicSourceDir = reference.path
            }
            ?: error("Cannot parse APK file: ${reference.path}")
    }

    private fun PerformanceTrace.markCacheHit() {
        this[CACHE_HIT_ATTRIBUTE] = true.toString()
    }
}

private suspend fun readNativeLibraries(applicationInfo: ApplicationInfo?, ioDispatcher: CoroutineDispatcher): NativeLibraries {
    val sourceDirs = listOfNotNull(applicationInfo?.sourceDir) + applicationInfo?.splitSourceDirs.orEmpty()
    val files = coroutineScope {
        sourceDirs.map { sourceDir -> async(ioDispatcher) { readNativeLibraryFiles(sourceDir) } }.awaitAll()
    }.flatten().distinctBy { it.name to it.abi }
    return if (files.isEmpty()) NativeLibraries.Empty else NativeLibraries(files)
}

private fun readNativeLibraryFiles(sourceDir: String): List<NativeLibraryFile> = runCatching {
    val containingApkFileName = File(sourceDir).name
    ZipFile(sourceDir).use { zip ->
        zip.entries().asSequence()
            .filter { it.name.startsWith(NATIVE_LIBRARY_ENTRY_PREFIX) }
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
