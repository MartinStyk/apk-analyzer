package sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RecentlyViewedAppDao {
    @Query("SELECT packageName FROM recently_viewed_apps ORDER BY lastViewedAt DESC LIMIT :limit")
    fun observeRecentPackageNames(limit: Int): Flow<List<String>>

    @Query(
        """
        INSERT INTO recently_viewed_apps (packageName, viewCount, lastViewedAt)
        VALUES (:packageName, 1, :timestamp)
        ON CONFLICT(packageName) DO UPDATE SET viewCount = viewCount + 1, lastViewedAt = :timestamp
        """,
    )
    suspend fun recordView(packageName: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM recently_viewed_apps")
    suspend fun count(): Int
}
