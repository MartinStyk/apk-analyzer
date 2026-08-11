package sk.styk.martin.apkanalyzer.core.appaidescription

import sk.styk.martin.apkanalyzer.core.common.model.AppReference

interface AppAiDescriptionRepository {
    suspend fun getDescription(reference: AppReference): AppAiDescription?
}
