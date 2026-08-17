package sk.styk.martin.apkanalyzer.core.apps.intentfilters

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.manifest.ManifestParser
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
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

private const val TAG = "IntentFiltersRepositoryImpl"
private const val CACHE_HIT_ATTRIBUTE = "cache_hit"

@Singleton
internal class IntentFiltersRepositoryImpl @Inject constructor(
    private val manifestParser: ManifestParser,
    packageChangesObserver: PackageChangesObserver,
    appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    private val performanceTracker: PerformanceTracker,
) : IntentFiltersRepository {

    private val cache = ConcurrentHashMap<AppReferenceCacheKey, Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>>()

    init {
        packageChangesObserver.observe()
            .onEach {
                Logger.d(TAG, "Package change detected, clearing cache")
                cache.clear()
            }
            .launchIn(appScope + dispatcherProvider.default())
    }

    override suspend fun componentIntentFilters(reference: AppReference): Result<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>> =
        performanceTracker.startCancellableTrace("intent_filters_load") {
            analysisMode = reference.analysisModeAttribute
            loadComponentIntentFilters(reference)
        }

    private suspend fun PerformanceTrace.loadComponentIntentFilters(
        reference: AppReference,
    ): Result<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>> {
        val cacheKey = reference.toCacheKey()
        cache[cacheKey]?.let {
            markCacheHit()
            outcome = TraceOutcome.Success
            return Result.success(it)
        }
        return manifestParser.componentIntentFilters(reference)
            .onSuccess { cache[cacheKey] = it }
            .also { outcome = if (it.isSuccess) TraceOutcome.Success else TraceOutcome.Error }
    }

    private fun PerformanceTrace.markCacheHit() {
        this[CACHE_HIT_ATTRIBUTE] = true.toString()
    }
}
