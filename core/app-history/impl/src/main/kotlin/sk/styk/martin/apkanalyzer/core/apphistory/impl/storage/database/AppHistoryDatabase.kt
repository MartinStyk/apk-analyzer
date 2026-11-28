package sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.entity.AppHistoryDeviceEntity
import sk.styk.martin.apkanalyzer.core.apphistory.impl.storage.database.entity.AppHistorySnapshotEntity

internal const val APP_HISTORY_DATABASE_NAME = "app_history_v2.db"
internal const val APP_HISTORY_STAGING_DATABASE_NAME = "app_history_v2_restore_staging.db"
internal const val APP_HISTORY_EXPORT_DATABASE_NAME = "app_history_v2_export.db"
internal const val APP_HISTORY_IMPORT_DATABASE_NAME = "app_history_v2_import.db"
internal val UNCOMPRESSED_APP_HISTORY_DATABASE_NAMES = listOf("app_history.db", "app_history_restore_staging.db")

@Database(
    entities = [AppHistorySnapshotEntity::class, AppHistoryBlobEntity::class, AppHistoryDeviceEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class AppHistoryDatabase : RoomDatabase() {
    abstract fun appHistoryGateDao(): AppHistoryGateDao
    abstract fun appHistoryWriteDao(): AppHistoryWriteDao
    abstract fun appHistoryReadDao(): AppHistoryReadDao
}
