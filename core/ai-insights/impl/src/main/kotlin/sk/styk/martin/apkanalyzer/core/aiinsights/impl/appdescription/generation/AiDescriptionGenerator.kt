package sk.styk.martin.apkanalyzer.core.aiinsights.impl.appdescription.generation

import sk.styk.martin.apkanalyzer.core.aiinsights.api.ai.AiAvailability
import sk.styk.martin.apkanalyzer.core.aiinsights.api.appdescription.AppAiDescription
import sk.styk.martin.apkanalyzer.core.aiinsights.impl.appdescription.AppAiContext

internal interface AiDescriptionGenerator {
    suspend fun checkAvailability(): AiAvailability

    suspend fun downloadModel(): Boolean

    fun inputHash(context: AppAiContext): String

    suspend fun generate(context: AppAiContext): AppAiDescription?
}
