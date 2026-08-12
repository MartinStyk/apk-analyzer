package sk.styk.martin.apkanalyzer.core.appaidescription.ai

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.collect
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OnDeviceAiEngine"

@Singleton
internal class OnDeviceAiEngineImpl @Inject constructor() : OnDeviceAiEngine {

    private val client: GenerativeModel by lazy { Generation.getClient() }

    override suspend fun isAvailable(): Boolean = runCatchingCancellable {
        when (client.checkStatus()) {
            FeatureStatus.AVAILABLE -> true
            FeatureStatus.DOWNLOADABLE -> downloadModel()
            else -> false
        }
    }.onFailure {
        Logger.w(TAG, it, "On-device AI availability check failed")
    }.getOrDefault(false)

    override suspend fun runPrompt(prompt: String): String? = runCatchingCancellable {
        client.generateContent(prompt).candidates.firstOrNull()?.text
    }.onFailure {
        Logger.w(TAG, it, "On-device AI generation failed")
    }.getOrNull()

    private suspend fun downloadModel(): Boolean {
        Logger.d(TAG, "On-device AI model download started")
        var downloadCompleted = false
        client.download().collect { status ->
            if (status is DownloadStatus.DownloadCompleted) {
                downloadCompleted = true
            }
        }
        Logger.d(TAG, "On-device AI model download finished: completed=$downloadCompleted")
        return downloadCompleted && client.checkStatus() == FeatureStatus.AVAILABLE
    }
}
