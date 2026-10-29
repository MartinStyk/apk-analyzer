package sk.styk.martin.apkanalyzer.core.apphistory.storage.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistorySnapshotEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistorySnapshotWithSectionsView

@Dao
internal interface AppHistoryReadDao {

    @Query("SELECT * FROM app_history_snapshot_with_sections WHERE packageName = :packageName ORDER BY capturedAt ASC, id ASC")
    fun observeSnapshotsWithSections(packageName: String): Flow<List<AppHistorySnapshotWithSectionsView>>

    @Query("SELECT * FROM app_history_snapshot")
    suspend fun allSnapshots(): List<AppHistorySnapshotEntity>

    @Query("SELECT * FROM app_history_blob")
    suspend fun allBlobs(): List<AppHistoryBlobEntity>
}
