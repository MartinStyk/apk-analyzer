package sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RecentlyViewedAppDao {
    @Query("SELECT packageName FROM recently_viewed_apps ORDER BY lastViewedAt DESC LIMIT :limit")
    fun observeRecentPackageNames(limit: Int): Flow<List<String>>

    @Transaction
    suspend fun recordView(packageName: String, timestamp: Long) {
        val currentViewCount = getViewCount(packageName) ?: 0
        upsert(RecentlyViewedAppEntity(packageName = packageName, viewCount = currentViewCount + 1, lastViewedAt = timestamp))
    }

    @Query("SELECT viewCount FROM recently_viewed_apps WHERE packageName = :packageName")
    suspend fun getViewCount(packageName: String): Int?

    @Upsert
    suspend fun upsert(entity: RecentlyViewedAppEntity)

    @Query("SELECT COUNT(*) FROM recently_viewed_apps")
    suspend fun count(): Int
}
