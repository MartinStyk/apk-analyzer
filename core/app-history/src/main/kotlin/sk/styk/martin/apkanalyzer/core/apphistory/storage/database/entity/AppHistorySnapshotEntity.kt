package sk.styk.martin.apkanalyzer.core.apphistory.storage.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_history_snapshot",
    foreignKeys = [ForeignKey(entity = AppHistoryDeviceEntity::class, parentColumns = ["deviceId"], childColumns = ["deviceId"])],
    indices = [
        Index(value = ["packageName", "capturedAt", "lastUpdateTime", "firstInstallTime"], unique = true),
        Index(value = ["deviceId"]),
    ],
)
internal data class AppHistorySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val deviceId: String,
    val capturedAt: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val applicationName: String,
    val processName: String?,
    val versionCode: Long,
    val versionName: String?,
    val isSystemApp: Boolean,
    val isDebuggable: Boolean,
    val allowsBackup: Boolean,
    val usesCleartextTraffic: Boolean,
    val uid: Int?,
    val sharedUserId: String?,
    val description: String?,
    val installLocation: String,
    val installingPackage: String?,
    val initiatingPackage: String?,
    val originatingPackage: String?,
    val apkSize: Long,
    val targetSdkVersion: Int?,
    val minSdkVersion: Int?,
    val permissionsHash: Long?,
    val activitiesHash: Long?,
    val servicesHash: Long?,
    val receiversHash: Long?,
    val providersHash: Long?,
    val featuresHash: Long?,
    val signingHash: Long?,
    val intentFiltersHash: Long?,
    val nativeLibrariesHash: Long?,
    val signingSchemeHash: Long?,
    val installedSplitsHash: Long?,
)
