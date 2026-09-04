package sk.styk.martin.apkanalyzer.core.apphistory.storage.entity

import androidx.room.Entity

@Entity(
    tableName = "app_history_blob",
    primaryKeys = ["packageName", "hash"],
)
internal data class AppHistoryBlobEntity(
    val packageName: String,
    val hash: String,
    val content: String,
)
