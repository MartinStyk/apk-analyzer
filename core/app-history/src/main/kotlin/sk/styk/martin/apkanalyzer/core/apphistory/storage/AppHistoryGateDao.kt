package sk.styk.martin.apkanalyzer.core.apphistory.storage

import androidx.room.Dao
import androidx.room.Query

internal data class AppHistoryGateState(
    val packageName: String,
    val lastUpdateTime: Long,
    val firstInstallTime: Long,
)

@Dao
internal interface AppHistoryGateDao {

    @Query("SELECT packageName, lastUpdateTime, firstInstallTime FROM app_history_snapshot WHERE packageName = :packageName ORDER BY id DESC LIMIT 1")
    suspend fun latestGateTimestamps(packageName: String): AppHistoryGateState?

    @Query(
        """
        SELECT packageName, lastUpdateTime, firstInstallTime
        FROM app_history_snapshot
        WHERE id IN (SELECT MAX(id) FROM app_history_snapshot GROUP BY packageName)
        """,
    )
    suspend fun latestGateTimestampsForAllPackages(): List<AppHistoryGateState>
}
