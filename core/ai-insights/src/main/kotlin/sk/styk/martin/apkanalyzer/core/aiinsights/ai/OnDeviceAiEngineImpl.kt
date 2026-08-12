package sk.styk.martin.apkanalyzer.core.aiinsights.ai

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.firstOrNull
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OnDeviceAiEngine"

@Singleton
internal class OnDeviceAiEngineImpl @Inject constructor(private val client: GenerativeModel) : OnDeviceAiEngine {

    override suspend fun checkAvailability(): AiAvailability = runCatchingCancellable {
        when (client.checkStatus()) {
            FeatureStatus.AVAILABLE -> AiAvailability.Available
            FeatureStatus.DOWNLOADABLE -> AiAvailability.Downloadable
            FeatureStatus.DOWNLOADING -> AiAvailability.Downloading
            else -> AiAvailability.Unavailable
        }
    }.onFailure {
        Logger.w(TAG, it, "On-device AI availability check failed")
    }.getOrDefault(AiAvailability.Unavailable)

    override suspend fun downloadModel(): Boolean = runCatchingCancellable {
        Logger.d(TAG, "On-device AI model download started")
        val downloadCompleted = client.download().firstOrNull { it is DownloadStatus.DownloadCompleted } != null
        Logger.d(TAG, "On-device AI model download finished: completed=$downloadCompleted")
        downloadCompleted && client.checkStatus() == FeatureStatus.AVAILABLE
    }.onFailure {
        Logger.w(TAG, it, "On-device AI model download failed")
    }.getOrDefault(false)

    override suspend fun runPrompt(prompt: String): String? = runCatchingCancellable {
        client.generateContent(prompt).candidates.firstOrNull()?.text
    }.onFailure {
        Logger.w(TAG, it, "On-device AI generation failed")
    }.getOrNull()
}
