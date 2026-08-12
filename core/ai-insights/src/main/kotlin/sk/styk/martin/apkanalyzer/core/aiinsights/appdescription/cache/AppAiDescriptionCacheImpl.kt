package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.cache

import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiDescription
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppAiDescriptionCacheImpl @Inject constructor(private val dao: AppAiDescriptionDao) : AppAiDescriptionCache {

    override suspend fun get(packageName: String, inputHash: String): AppAiDescription? = dao.get(packageName, inputHash)
        ?.let { AppAiDescription(description = it.description) }

    override suspend fun save(
        packageName: String,
        inputHash: String,
        description: AppAiDescription,
    ) {
        dao.upsert(
            AppAiDescriptionEntity(
                packageName = packageName,
                inputHash = inputHash,
                description = description.description,
                generatedAt = Instant.now().toEpochMilli(),
            ),
        )
    }
}
