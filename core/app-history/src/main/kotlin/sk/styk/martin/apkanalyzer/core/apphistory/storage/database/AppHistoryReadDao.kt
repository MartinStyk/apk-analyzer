package sk.styk.martin.apkanalyzer.core.apphistory.storage.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistoryDeviceEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistorySnapshotEntity

@Dao
internal interface AppHistoryReadDao {

    @Query("SELECT * FROM app_history_snapshot WHERE packageName = :packageName ORDER BY capturedAt ASC, id ASC")
    fun observeSnapshots(packageName: String): Flow<List<AppHistorySnapshotEntity>>

    @Query("SELECT * FROM app_history_blob WHERE packageName = :packageName")
    suspend fun blobs(packageName: String): List<AppHistoryBlobEntity>

    @Query("SELECT * FROM app_history_snapshot")
    suspend fun allSnapshots(): List<AppHistorySnapshotEntity>

    @Query("SELECT * FROM app_history_blob")
    suspend fun allBlobs(): List<AppHistoryBlobEntity>

    @Query("SELECT * FROM app_history_device")
    suspend fun allDevices(): List<AppHistoryDeviceEntity>
}
