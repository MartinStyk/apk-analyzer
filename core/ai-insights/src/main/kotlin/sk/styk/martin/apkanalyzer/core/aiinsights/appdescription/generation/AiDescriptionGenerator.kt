package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.generation

import sk.styk.martin.apkanalyzer.core.aiinsights.ai.AiAvailability
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiDescription

internal interface AiDescriptionGenerator {
    suspend fun checkAvailability(): AiAvailability

    suspend fun downloadModel(): Boolean

    fun inputHash(context: AppAiContext): String

    suspend fun generate(context: AppAiContext): AppAiDescription?
}
