package sk.styk.martin.apkanalyzer.core.aiinsights.ai

import android.os.Build
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerativeModel
import dagger.Lazy
import kotlinx.coroutines.flow.firstOrNull
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OnDeviceAiEngine"

@Singleton
internal class OnDeviceAiEngineImpl @Inject constructor(private val client: Lazy<GenerativeModel>) : OnDeviceAiEngine {

    override suspend fun checkAvailability(): AiAvailability {
        if (isRunningOnEmulator()) {
            Logger.d(TAG, "On-device AI unavailable: running on an emulator")
            return AiAvailability.Unavailable
        }
        return runCatchingCancellable {
            when (client.get().checkStatus()) {
                FeatureStatus.AVAILABLE -> AiAvailability.Available
                FeatureStatus.DOWNLOADABLE -> AiAvailability.Downloadable
                FeatureStatus.DOWNLOADING -> AiAvailability.Downloading
                else -> AiAvailability.Unavailable
            }
        }.onFailure {
            Logger.w(TAG, it, "On-device AI availability check failed")
        }.getOrDefault(AiAvailability.Unavailable)
    }

    override suspend fun downloadModel(): Boolean = runCatchingCancellable {
        Logger.d(TAG, "On-device AI model download started")
        val downloadCompleted = client.get().download().firstOrNull { it is DownloadStatus.DownloadCompleted } != null
        Logger.d(TAG, "On-device AI model download finished: completed=$downloadCompleted")
        downloadCompleted && client.get().checkStatus() == FeatureStatus.AVAILABLE
    }.onFailure {
        Logger.w(TAG, it, "On-device AI model download failed")
    }.getOrDefault(false)

    override suspend fun runPrompt(prompt: String): String? = runCatchingCancellable {
        client.get().generateContent(prompt).candidates.firstOrNull()?.text
    }.onFailure {
        Logger.w(TAG, it, "On-device AI generation failed")
    }.getOrNull()
}

private fun isRunningOnEmulator(): Boolean = Build.FINGERPRINT.startsWith("generic") ||
    Build.FINGERPRINT.startsWith("unknown") ||
    Build.MODEL.contains("google_sdk") ||
    Build.MODEL.contains("sdk_gphone") ||
    Build.MODEL.contains("Emulator") ||
    Build.MODEL.contains("Android SDK built for") ||
    Build.MANUFACTURER.contains("Genymotion") ||
    Build.PRODUCT.contains("sdk_gphone") ||
    Build.HARDWARE.contains("goldfish") ||
    Build.HARDWARE.contains("ranchu") ||
    (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
