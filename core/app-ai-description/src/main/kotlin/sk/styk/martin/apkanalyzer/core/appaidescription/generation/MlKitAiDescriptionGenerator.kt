package sk.styk.martin.apkanalyzer.core.appaidescription.generation

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.collect
import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescription
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MlKitAiDescriptionGenerator"

@Singleton
internal class MlKitAiDescriptionGenerator @Inject constructor(private val promptBuilder: PromptBuilder, private val descriptionParser: DescriptionParser) :
    AiDescriptionGenerator {

    private val client: GenerativeModel by lazy { Generation.getClient() }

    override suspend fun isAvailable(): Boolean = runCatchingCancellable {
        when (client.checkStatus()) {
            FeatureStatus.AVAILABLE -> true

            FeatureStatus.DOWNLOADABLE -> {
                Logger.d(TAG, "On-device AI model download started")
                var downloadCompleted = false
                client.download().collect { status ->
                    if (status is DownloadStatus.DownloadCompleted) {
                        downloadCompleted = true
                    }
                }
                Logger.d(TAG, "On-device AI model download finished: completed=$downloadCompleted")
                downloadCompleted && client.checkStatus() == FeatureStatus.AVAILABLE
            }

            else -> false
        }
    }.onFailure {
        Logger.w(TAG, it, "On-device AI availability check failed")
    }.getOrDefault(false)

    override suspend fun generate(context: AppAiContext, strict: Boolean): AppAiDescription? = runCatchingCancellable {
        val prompt = promptBuilder.build(context, strict)
        val response = client.generateContent(prompt)
        val text = response.candidates.firstOrNull()?.text ?: return@runCatchingCancellable null
        descriptionParser.parse(text)
    }.onFailure {
        Logger.w(TAG, it, "On-device AI generation failed")
    }.getOrNull()
}
