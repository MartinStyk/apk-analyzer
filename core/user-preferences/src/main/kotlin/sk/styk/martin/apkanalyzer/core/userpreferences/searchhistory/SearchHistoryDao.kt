package sk.styk.martin.apkanalyzer.core.userpreferences.searchhistory

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY lastSearchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SearchHistoryEntity>>

    @Transaction
    suspend fun recordSearch(
        packageName: String,
        query: String,
        timestamp: Long,
    ) {
        val currentSearchCount = getSearchCount(packageName) ?: 0
        upsert(
            SearchHistoryEntity(
                packageName = packageName,
                query = query,
                searchCount = currentSearchCount + 1,
                lastSearchedAt = timestamp,
            ),
        )
    }

    @Query("SELECT searchCount FROM search_history WHERE packageName = :packageName")
    suspend fun getSearchCount(packageName: String): Int?

    @Upsert
    suspend fun upsert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE packageName = :packageName")
    suspend fun deletePackage(packageName: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
