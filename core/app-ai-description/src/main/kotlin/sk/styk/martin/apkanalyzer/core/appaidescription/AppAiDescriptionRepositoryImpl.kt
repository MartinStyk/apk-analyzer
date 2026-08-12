package sk.styk.martin.apkanalyzer.core.appaidescription

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sk.styk.martin.apkanalyzer.core.appaidescription.cache.AppAiDescriptionCache
import sk.styk.martin.apkanalyzer.core.appaidescription.generation.AiDescriptionGenerator
import sk.styk.martin.apkanalyzer.core.appaidescription.generation.DescriptionValidator
import sk.styk.martin.apkanalyzer.core.appaidescription.metadata.AppMetadataProvider
import sk.styk.martin.apkanalyzer.core.common.digest.DigestManager
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppAiDescriptionRepository"

@Singleton
internal class AppAiDescriptionRepositoryImpl @Inject constructor(
    private val metadataProvider: AppMetadataProvider,
    private val cache: AppAiDescriptionCache,
    private val generator: AiDescriptionGenerator,
    private val validator: DescriptionValidator,
    private val digestManager: DigestManager,
    private val appScope: CoroutineScope,
) : AppAiDescriptionRepository {

    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<AppReference, Deferred<AppAiDescription?>>()

    override suspend fun getDescription(reference: AppReference): AppAiDescription? {
        val deferred = inFlightMutex.withLock {
            inFlight.getOrPut(reference) { startGeneration(reference) }
        }
        return deferred.await()
    }

    private fun startGeneration(reference: AppReference): Deferred<AppAiDescription?> {
        lateinit var deferred: Deferred<AppAiDescription?>
        deferred = appScope.async { loadDescription(reference) }
        deferred.invokeOnCompletion {
            appScope.launch {
                inFlightMutex.withLock {
                    inFlight.remove(reference, deferred)
                }
            }
        }
        return deferred
    }

    private suspend fun loadDescription(reference: AppReference): AppAiDescription? {
        Logger.d(TAG, "AI description loading started: reference=$reference")
        val context = fetchContext(reference) ?: return null
        val inputHash = calculateInputHash(context)
        return getCachedDescription(context, inputHash, reference) ?: generateAndCacheDescription(context, inputHash, reference)
    }

    private fun calculateInputHash(context: AppAiContext): String =
        digestManager.sha256Digest("promptVersion=${generator.promptVersion}\n${context.serialize()}")

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
    ): AppAiDescription? {
        val description = generateIfAvailable(context, reference) ?: return null
        cache.save(context.packageName, inputHash, description)
        Logger.i(TAG, "AI description loading finished: generated, reference=$reference")
        return description
    }

    private suspend fun generateIfAvailable(context: AppAiContext, reference: AppReference): AppAiDescription? {
        if (!generator.isAvailable()) {
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
        val firstAttempt = generator.generate(context, strict = false)
        if (firstAttempt != null && validator.isValid(firstAttempt, context)) {
            return firstAttempt
        }

        val retryAttempt = generator.generate(context, strict = true)
        return retryAttempt?.takeIf { validator.isValid(it, context) }
    }
}
