package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.generation

import sk.styk.martin.apkanalyzer.core.aiinsights.ai.AiAvailability
import sk.styk.martin.apkanalyzer.core.aiinsights.ai.OnDeviceAiEngine
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiDescription
import sk.styk.martin.apkanalyzer.core.common.digest.DigestManager
import javax.inject.Inject

internal class AiDescriptionGeneratorImpl @Inject constructor(
    private val onDeviceAiEngine: OnDeviceAiEngine,
    private val promptBuilder: PromptBuilder,
    private val descriptionParser: DescriptionParser,
    private val digestManager: DigestManager,
) : AiDescriptionGenerator {

    override suspend fun checkAvailability(): AiAvailability = onDeviceAiEngine.checkAvailability()

    override suspend fun downloadModel(): Boolean = onDeviceAiEngine.downloadModel()

    override fun inputHash(context: AppAiContext): String = digestManager.sha256Digest(promptBuilder.build(context))

    override suspend fun generate(context: AppAiContext): AppAiDescription? {
        val rawOutput = onDeviceAiEngine.runPrompt(promptBuilder.build(context)) ?: return null
        return descriptionParser.parse(rawOutput)
    }
}
