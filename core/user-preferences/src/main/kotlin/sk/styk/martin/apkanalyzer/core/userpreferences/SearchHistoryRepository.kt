package sk.styk.martin.apkanalyzer.core.userpreferences

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun queries(): Flow<List<String>>
    suspend fun addQuery(query: String)
    suspend fun removeQuery(query: String)
    suspend fun clearAll()
}
