package sk.styk.martin.apkanalyzer.core.apphistory.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import sk.styk.martin.apkanalyzer.core.apphistory.storage.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.entity.AppHistorySnapshotEntity

@Dao
internal interface AppHistoryWriteDao {

    @Transaction
    suspend fun insertSnapshotWithBlobs(snapshot: AppHistorySnapshotEntity, blobs: List<AppHistoryBlobEntity>) {
        insertBlobs(blobs)
        insertSnapshot(snapshot)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlobs(blobs: List<AppHistoryBlobEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(snapshot: AppHistorySnapshotEntity)
}
