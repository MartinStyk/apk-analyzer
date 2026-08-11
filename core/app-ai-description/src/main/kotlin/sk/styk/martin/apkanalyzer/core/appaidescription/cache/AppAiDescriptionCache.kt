package sk.styk.martin.apkanalyzer.core.appaidescription.cache

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescription

internal interface AppAiDescriptionCache {
    suspend fun get(
        packageName: String,
        versionCode: Long,
        inputHash: String,
    ): AppAiDescription?

    suspend fun save(
        packageName: String,
        versionCode: Long,
        inputHash: String,
        description: AppAiDescription,
    )
}
