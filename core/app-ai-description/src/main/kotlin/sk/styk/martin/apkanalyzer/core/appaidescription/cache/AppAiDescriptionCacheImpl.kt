package sk.styk.martin.apkanalyzer.core.appaidescription.cache

import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescription
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppAiDescriptionCacheImpl @Inject constructor(private val dao: AppAiDescriptionDao) : AppAiDescriptionCache {

    override suspend fun get(packageName: String, inputHash: String): AppAiDescription? = dao.get(packageName, inputHash)
        ?.let { AppAiDescription(shortDescription = it.shortDescription, longDescription = it.longDescription) }

    override suspend fun save(packageName: String, inputHash: String, description: AppAiDescription) {
        dao.upsert(
            AppAiDescriptionEntity(
                packageName = packageName,
                inputHash = inputHash,
                shortDescription = description.shortDescription,
                longDescription = description.longDescription,
                generatedAt = Instant.now().toEpochMilli(),
            ),
        )
    }
}
