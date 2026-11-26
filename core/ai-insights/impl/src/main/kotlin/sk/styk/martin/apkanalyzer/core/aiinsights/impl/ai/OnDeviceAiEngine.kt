package sk.styk.martin.apkanalyzer.core.aiinsights.impl.ai

import sk.styk.martin.apkanalyzer.core.aiinsights.api.ai.AiAvailability

internal interface OnDeviceAiEngine {
    suspend fun checkAvailability(): AiAvailability

    suspend fun downloadModel(): Boolean

    suspend fun runPrompt(prompt: String): String?
}
