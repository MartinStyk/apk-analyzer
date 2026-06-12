package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail

@Immutable
sealed interface AppDetailState {
    data object Loading : AppDetailState

    @Immutable
    data class Loaded(
        val analysisMode: AppDetail.AnalysisMode,
        val appName: String,
        val packageName: String,
        val processName: String?,
        val versionName: String?,
        val versionCode: Long,
        val uid: Int?,
        val description: String?,
        val isSystemApp: Boolean,
        val source: String,
        val apkDirectory: String?,
        val dataDirectory: String?,
        val apkSize: Long,
        val totalSize: Long? = null,
        val targetSdkVersion: Int?,
        val targetSdkLabel: String?,
        val minSdkVersion: Int?,
        val minSdkLabel: String?,
        val installLocation: String,
        val appInstaller: String?,
        val firstInstallTime: Long?,
        val lastUpdateTime: Long?,
        val totalPermissionsCount: Int,
        val dangerousPermissionsCount: Int,
        val definedPermissionsCount: Int,
        val activitiesCount: Int,
        val servicesCount: Int,
        val contentProvidersCount: Int,
        val broadcastReceiversCount: Int,
        val certificatesCount: Int,
        val featuresCount: Int,
    ) : AppDetailState {
        val isTargetSdkOutdated: Boolean
            get() = targetSdkVersion != null && targetSdkVersion < 29

        val isSourceUnknown: Boolean
            get() = source == "Unknown"
    }

    data object Error : AppDetailState
}
