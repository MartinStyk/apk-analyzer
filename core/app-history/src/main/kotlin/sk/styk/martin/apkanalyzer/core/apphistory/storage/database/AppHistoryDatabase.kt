package sk.styk.martin.apkanalyzer.core.apphistory.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistoryDeviceEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistorySnapshotEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity.AppHistorySnapshotWithSectionsView

internal const val APP_HISTORY_DATABASE_NAME = "app_history_v2.db"
internal const val APP_HISTORY_STAGING_DATABASE_NAME = "app_history_v2_restore_staging.db"
internal val UNCOMPRESSED_APP_HISTORY_DATABASE_NAMES = listOf("app_history.db", "app_history_restore_staging.db")

@Database(
    entities = [AppHistorySnapshotEntity::class, AppHistoryBlobEntity::class, AppHistoryDeviceEntity::class],
    views = [AppHistorySnapshotWithSectionsView::class],
    version = 1,
    exportSchema = true,
)
internal abstract class AppHistoryDatabase : RoomDatabase() {
    abstract fun appHistoryGateDao(): AppHistoryGateDao
    abstract fun appHistoryWriteDao(): AppHistoryWriteDao
    abstract fun appHistoryReadDao(): AppHistoryReadDao
}
