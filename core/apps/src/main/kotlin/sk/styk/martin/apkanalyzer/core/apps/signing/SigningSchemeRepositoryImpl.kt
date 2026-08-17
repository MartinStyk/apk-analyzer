package sk.styk.martin.apkanalyzer.core.apps.signing

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.AppReferenceCacheKey
import sk.styk.martin.apkanalyzer.core.common.model.toCacheKey
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrace
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.analysisMode
import sk.styk.martin.apkanalyzer.core.common.performance.analysisModeAttribute
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SigningSchemeRepositoryImpl"
private const val CACHE_HIT_ATTRIBUTE = "cache_hit"

@Singleton
internal class SigningSchemeRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val apkSigningBlockAnalyzer: ApkSigningBlockAnalyzer,
    packageChangesObserver: PackageChangesObserver,
    appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val performanceTracker: PerformanceTracker,
) : SigningSchemeRepository {

    private val cache = ConcurrentHashMap<AppReferenceCacheKey, SigningSchemeResult>()

    init {
        packageChangesObserver.observe()
            .onEach {
                Logger.d(TAG, "Package change detected, clearing cache")
                cache.clear()
            }
            .launchIn(appScope + dispatcherProvider.default())
    }

    override suspend fun signingSchemeVersions(reference: AppReference): Result<List<SigningSchemeVersion>?> =
        performanceTracker.startCancellableTrace("signing_scheme_load") {
            analysisMode = reference.analysisModeAttribute
            val cacheKey = reference.toCacheKey()
            cache[cacheKey]?.let {
                markCacheHit()
                outcome = TraceOutcome.Success
                return@startCancellableTrace Result.success(it.versions)
            }
            runCatchingCancellable {
                val applicationInfo = resolveApplicationInfo(reference)
                withContext(dispatcherProvider.io()) {
                    applicationInfo.sourceDir?.let(apkSigningBlockAnalyzer::detectSchemeVersions)
                }
            }.onSuccess {
                cache[cacheKey] = SigningSchemeResult(it)
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

private data class SigningSchemeResult(val versions: List<SigningSchemeVersion>?)
