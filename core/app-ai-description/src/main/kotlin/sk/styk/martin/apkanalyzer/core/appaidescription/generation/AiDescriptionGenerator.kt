package sk.styk.martin.apkanalyzer.core.appaidescription.generation

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescription

internal interface AiDescriptionGenerator {
    val promptVersion: Int

    suspend fun isAvailable(): Boolean

    suspend fun generate(context: AppAiContext, strict: Boolean): AppAiDescription?
}
