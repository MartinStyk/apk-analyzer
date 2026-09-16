package sk.styk.martin.apkanalyzer.core.apphistory.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import sk.styk.martin.apkanalyzer.core.apphistory.storage.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.entity.AppHistorySnapshotEntity

@Database(
    entities = [AppHistorySnapshotEntity::class, AppHistoryBlobEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class AppHistoryDatabase : RoomDatabase() {
    abstract fun appHistoryGateDao(): AppHistoryGateDao
    abstract fun appHistoryWriteDao(): AppHistoryWriteDao
    abstract fun appHistoryReadDao(): AppHistoryReadDao
}
