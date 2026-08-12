package sk.styk.martin.apkanalyzer.core.appaidescription.ai

internal interface OnDeviceAiEngine {
    suspend fun isAvailable(): Boolean

    suspend fun runPrompt(prompt: String): String?
}
