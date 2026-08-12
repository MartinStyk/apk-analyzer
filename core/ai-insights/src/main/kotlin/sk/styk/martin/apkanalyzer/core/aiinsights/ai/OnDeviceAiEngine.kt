package sk.styk.martin.apkanalyzer.core.aiinsights.ai

internal interface OnDeviceAiEngine {
    suspend fun checkAvailability(): AiAvailability

    suspend fun downloadModel(): Boolean

    suspend fun runPrompt(prompt: String): String?
}
