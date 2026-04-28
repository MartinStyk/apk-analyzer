package sk.styk.martin.apkanalyzer.core.applist

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject

internal class SearchHistoryRepositoryImpl @Inject constructor(private val persistenceRepository: PersistenceRepository) : SearchHistoryRepository {

    override fun queries(): Flow<List<String>> = persistenceRepository.observe(Key.SearchHistory)

    override suspend fun addQuery(query: String) {
        val current = persistenceRepository.get(Key.SearchHistory)
        val updated = (listOf(query) + current.filter { it != query }).take(MAX_HISTORY)
        persistenceRepository.save(Key.SearchHistory, updated)
    }

    override suspend fun removeQuery(query: String) {
        val current = persistenceRepository.get(Key.SearchHistory)
        persistenceRepository.save(Key.SearchHistory, current.filter { it != query })
    }

    override suspend fun clearAll() {
        persistenceRepository.save(Key.SearchHistory, emptyList())
    }

    private companion object {
        const val MAX_HISTORY = 15
    }
}
