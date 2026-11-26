package sk.styk.martin.apkanalyzer.core.aiinsights.impl.appdescription.cache

import sk.styk.martin.apkanalyzer.core.aiinsights.api.appdescription.AppAiDescription

internal interface AppAiDescriptionCache {
    suspend fun get(packageName: String, inputHash: String): AppAiDescription?

    suspend fun save(
        packageName: String,
        inputHash: String,
        description: AppAiDescription,
    )
}
