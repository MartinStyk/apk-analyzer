package sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_history_device")
internal data class AppHistoryDeviceEntity(
    @PrimaryKey val deviceId: String,
    val firstSeenAt: Long,
    val manufacturer: String,
    val model: String,
)
