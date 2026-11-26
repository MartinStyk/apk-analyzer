package sk.styk.martin.apkanalyzer.core.aiinsights.api.appdescription

import kotlinx.coroutines.flow.StateFlow
import sk.styk.martin.apkanalyzer.core.aiinsights.api.ai.AiAvailability
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

interface AppAiDescriptionRepository {
    val availability: StateFlow<AiAvailability>

    fun downloadModel()

    suspend fun getDescription(reference: AppReference): AppAiDescription?
}
