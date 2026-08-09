package sk.styk.martin.apkanalyzer.feature.appdetail.impl.generalinfo

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

@Immutable
sealed interface GeneralInfoState {
    data object Loading : GeneralInfoState

    @Immutable
    data class Loaded(
        val applicationName: String,
        val packageName: PackageName,
        val processName: String?,
        val uid: Int?,
        val sharedUserId: String?,
        val description: String?,
        val versionName: String?,
        val versionCode: Long,
        val minSdkVersion: Int?,
        val minSdkLabel: String?,
        val targetSdkVersion: Int?,
        val targetSdkLabel: String?,
        val isSystemApp: Boolean,
        val isDebuggable: Boolean,
        val allowsBackup: Boolean,
        val usesCleartextTraffic: Boolean,
        val source: AppSource,
        val appInstaller: PackageName?,
        val firstInstallTime: Instant?,
        val lastUpdateTime: Instant?,
        val lastUsedTime: Instant?,
        val apkDirectory: String?,
        val dataDirectory: String?,
        val installLocation: String,
        val apkSize: AppSize,
        val totalSize: AppSize?,
    ) : GeneralInfoState

    data object Error : GeneralInfoState
}
