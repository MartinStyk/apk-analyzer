package sk.styk.martin.apkanalyzer.core.apphistory.storage.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_history_snapshot",
    indices = [Index(value = ["packageName", "lastUpdateTime", "firstInstallTime"], unique = true)],
)
internal data class AppHistorySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val deviceId: String,
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
    val permissionsHash: String?,
    val activitiesHash: String?,
    val servicesHash: String?,
    val receiversHash: String?,
    val providersHash: String?,
    val featuresHash: String?,
    val signingHash: String?,
    val intentFiltersHash: String?,
    val nativeLibrariesHash: String?,
    val signingSchemeHash: String?,
    val installedSplitsHash: String?,
)
