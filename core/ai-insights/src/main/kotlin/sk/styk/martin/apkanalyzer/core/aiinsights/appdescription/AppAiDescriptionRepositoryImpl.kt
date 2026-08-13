package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sk.styk.martin.apkanalyzer.core.aiinsights.ai.AiAvailability
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.cache.AppAiDescriptionCache
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.generation.AiDescriptionGenerator
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.generation.DescriptionValidator
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.metadata.AppMetadataProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrace
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppAiDescriptionRepository"

@Singleton
internal class AppAiDescriptionRepositoryImpl @Inject constructor(
    private val metadataProvider: AppMetadataProvider,
    private val cache: AppAiDescriptionCache,
    private val generator: AiDescriptionGenerator,
    private val validator: DescriptionValidator,
    private val appScope: CoroutineScope,
    private val performanceTracker: PerformanceTracker,
) : AppAiDescriptionRepository {

    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<AppReference, Deferred<AppAiDescription?>>()

    final override val availability: StateFlow<AiAvailability>
        field = MutableStateFlow(AiAvailability.Unavailable)

    private var downloadJob: Job? = null

    init {
        appScope.launch { refreshAvailability() }
    }

    @Synchronized
    override fun downloadModel() {
        if (downloadJob?.isActive == true) return
        downloadJob = appScope.launch {
            availability.value = AiAvailability.Downloading
            generator.downloadModel()
            refreshAvailability()
        }
    }

    private suspend fun refreshAvailability() {
        availability.value = generator.checkAvailability()
    }

    override suspend fun getDescription(reference: AppReference): AppAiDescription? {
        val deferred = inFlightMutex.withLock {
            inFlight.getOrPut(reference) { startGeneration(reference) }
        }
        return deferred.await()
    }

    private fun startGeneration(reference: AppReference): Deferred<AppAiDescription?> {
        val deferred: Deferred<AppAiDescription?> = appScope.async { loadDescription(reference) }
        deferred.invokeOnCompletion {
            appScope.launch {
                inFlightMutex.withLock {
                    inFlight.remove(reference, deferred)
                }
            }
        }
        return deferred
    }

    private suspend fun loadDescription(reference: AppReference): AppAiDescription? = performanceTracker.startCancellableTrace("ai_summary_load") {
        this["analysis_mode"] = reference.toString()
        Logger.d(TAG, "AI description loading started: reference=$reference")
        runCatchingCancellable {
            val context = fetchContext(reference) ?: return@runCatchingCancellable null
            val isCacheable = reference is AppReference.InstalledPackage
            val inputHash = generator.inputHash(context)
            val cached = if (isCacheable) getCachedDescription(context, inputHash, reference) else null
            this["cache_hit"] = (cached != null).toString()
            cached ?: generateAndCacheDescription(context, inputHash, reference, isCacheable, this)
        }.fold(
            onSuccess = { description ->
                outcome = if (description != null) TraceOutcome.Success else TraceOutcome.Degraded
                description
            },
            onFailure = { failure ->
                outcome = TraceOutcome.Error
                Logger.e(TAG, failure, "AI description loading failed")
                null
            },
        )
    }

    private suspend fun fetchContext(reference: AppReference): AppAiContext? {
        val context = metadataProvider.getAppContext(reference)
        if (context == null) {
            Logger.w(TAG, "AI description loading failed: metadata unavailable, reference=$reference")
        }
        return context
    }

    private suspend fun getCachedDescription(
        context: AppAiContext,
        inputHash: String,
        reference: AppReference,
    ): AppAiDescription? {
        val cached = cache.get(context.packageName, inputHash)
        if (cached != null) {
            Logger.d(TAG, "AI description loading finished: cache hit, reference=$reference")
        }
        return cached
    }

    private suspend fun generateAndCacheDescription(
        context: AppAiContext,
        inputHash: String,
        reference: AppReference,
        isCacheable: Boolean,
        trace: PerformanceTrace,
    ): AppAiDescription? {
        val description = generateIfAvailable(context, reference, trace) ?: return null
        if (isCacheable) {
            cache.save(context.packageName, inputHash, description)
        }
        Logger.i(TAG, "AI description loading finished: generated, reference=$reference")
        return description
    }

    private suspend fun generateIfAvailable(
        context: AppAiContext,
        reference: AppReference,
        trace: PerformanceTrace,
    ): AppAiDescription? {
        trace["availability"] = availability.value.name.lowercase()
        if (availability.value != AiAvailability.Available) {
            Logger.d(TAG, "AI description loading finished: AI unavailable, reference=$reference")
            return null
        }
        val description = generateValidDescription(context)
        if (description == null) {
            Logger.w(TAG, "AI description loading failed: no valid output, reference=$reference")
        }
        return description
    }

    private suspend fun generateValidDescription(context: AppAiContext): AppAiDescription? {
        val firstAttempt = generator.generate(context)
        if (firstAttempt != null && validator.isValid(firstAttempt, context)) {
            return firstAttempt
        }

        val retryAttempt = generator.generate(context)
        return retryAttempt?.takeIf { validator.isValid(it, context) }
    }
}
