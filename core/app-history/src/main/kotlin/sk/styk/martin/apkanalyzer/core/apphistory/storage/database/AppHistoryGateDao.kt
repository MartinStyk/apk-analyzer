package sk.styk.martin.apkanalyzer.core.apphistory.storage.database

import androidx.room.Dao
import androidx.room.Query

internal data class AppHistoryGateState(
    val packageName: String,
    val deviceId: String,
    val lastUpdateTime: Long,
    val firstInstallTime: Long,
    val versionCode: Long,
    val capturedAt: Long,
)

@Dao
internal interface AppHistoryGateDao {

    @Query(
        """
        SELECT packageName, deviceId, lastUpdateTime, firstInstallTime, versionCode, capturedAt
        FROM app_history_snapshot
        WHERE packageName = :packageName
        ORDER BY capturedAt DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun latestGateState(packageName: String): AppHistoryGateState?

    @Query(
        """
        SELECT packageName, deviceId, lastUpdateTime, firstInstallTime, versionCode, MAX(capturedAt) AS capturedAt
        FROM app_history_snapshot
        GROUP BY packageName
        """,
    )
    suspend fun latestGateStatesForAllPackages(): List<AppHistoryGateState>
}
