package sk.styk.martin.apkanalyzer.core.userpreferences.searchhistory

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY lastSearchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SearchHistoryEntity>>

    @Query(
        """
        INSERT INTO search_history (packageName, query, searchCount, lastSearchedAt)
        VALUES (:packageName, :query, 1, :timestamp)
        ON CONFLICT(packageName) DO UPDATE SET
            query = :query,
            searchCount = searchCount + 1,
            lastSearchedAt = :timestamp
        """,
    )
    suspend fun recordSearch(
        packageName: String,
        query: String,
        timestamp: Long,
    )

    @Query("DELETE FROM search_history WHERE packageName = :packageName")
    suspend fun deletePackage(packageName: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
