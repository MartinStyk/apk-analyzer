package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.metadata

import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

internal interface AppMetadataProvider {
    suspend fun getAppContext(reference: AppReference): AppAiContext?
}
