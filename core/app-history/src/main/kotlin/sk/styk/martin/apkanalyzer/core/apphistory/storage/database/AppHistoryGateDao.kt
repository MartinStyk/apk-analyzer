package sk.styk.martin.apkanalyzer.core.apphistory.storage.database

import androidx.room.Dao
import androidx.room.Query

internal data class AppHistoryGateState(
    val packageName: String,
    val deviceId: String,
    val lastUpdateTime: Long,
    val firstInstallTime: Long,
)

@Dao
internal interface AppHistoryGateDao {

    @Query(
        """
        SELECT packageName, deviceId, lastUpdateTime, firstInstallTime
        FROM app_history_snapshot
        WHERE packageName = :packageName
        ORDER BY capturedAt DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun latestGateTimestamps(packageName: String): AppHistoryGateState?

    @Query(
        """
        SELECT packageName, deviceId, lastUpdateTime, firstInstallTime
        FROM app_history_snapshot s1
        WHERE NOT EXISTS (
            SELECT 1 FROM app_history_snapshot s2
            WHERE s2.packageName = s1.packageName
            AND (s2.capturedAt > s1.capturedAt OR (s2.capturedAt = s1.capturedAt AND s2.id > s1.id))
        )
        """,
    )
    suspend fun latestGateTimestampsForAllPackages(): List<AppHistoryGateState>
}
