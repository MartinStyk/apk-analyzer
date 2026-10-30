package sk.styk.martin.apkanalyzer.core.apphistory.storage.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistoryDeviceEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistorySnapshotEntity

@Dao
internal interface AppHistoryWriteDao {

    @Transaction
    suspend fun insertSnapshotWithBlobs(
        device: AppHistoryDeviceEntity,
        snapshot: AppHistorySnapshotEntity,
        blobs: List<AppHistoryBlobEntity>,
    ) {
        insertDevices(listOf(device))
        insertBlobs(blobs)
        insertSnapshot(snapshot)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlobs(blobs: List<AppHistoryBlobEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(snapshot: AppHistorySnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDevices(devices: List<AppHistoryDeviceEntity>)

    @Transaction
    suspend fun mergeRestoredData(
        snapshots: List<AppHistorySnapshotEntity>,
        blobs: List<AppHistoryBlobEntity>,
        devices: List<AppHistoryDeviceEntity>,
    ) {
        insertDevices(devices)
        insertBlobs(blobs)
        insertSnapshotsIgnoringConflicts(snapshots)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSnapshotsIgnoringConflicts(snapshots: List<AppHistorySnapshotEntity>)
}
