package sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity

import androidx.room.Entity

@Entity(
    tableName = "app_history_blob",
    primaryKeys = ["packageName", "hash"],
)
internal class AppHistoryBlobEntity(
    val packageName: String,
    val hash: String,
    val content: ByteArray,
)
