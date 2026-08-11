package sk.styk.martin.apkanalyzer.core.appaidescription.metadata

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiContext
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

internal interface AppMetadataProvider {
    suspend fun getAppContext(reference: AppReference): AppAiContext?
}
