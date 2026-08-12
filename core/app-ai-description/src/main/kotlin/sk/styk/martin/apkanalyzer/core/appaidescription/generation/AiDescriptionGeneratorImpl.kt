package sk.styk.martin.apkanalyzer.core.appaidescription.generation

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescription
import sk.styk.martin.apkanalyzer.core.appaidescription.ai.OnDeviceAiEngine
import javax.inject.Inject

internal class AiDescriptionGeneratorImpl @Inject constructor(
    private val onDeviceAiEngine: OnDeviceAiEngine,
    private val promptBuilder: PromptBuilder,
    private val descriptionParser: DescriptionParser,
) : AiDescriptionGenerator {

    override val promptVersion = PROMPT_VERSION

    override suspend fun isAvailable(): Boolean = onDeviceAiEngine.isAvailable()

    override suspend fun generate(context: AppAiContext, strict: Boolean): AppAiDescription? {
        val rawOutput = onDeviceAiEngine.runPrompt(promptBuilder.build(context, strict)) ?: return null
        return descriptionParser.parse(rawOutput)
    }
}
