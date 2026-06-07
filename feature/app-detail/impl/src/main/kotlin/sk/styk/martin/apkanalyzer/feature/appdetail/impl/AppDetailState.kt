package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetailData

@Immutable
sealed interface AppDetailState {
    data object Loading : AppDetailState

    @Immutable
    data class Loaded(
        val analysisMode: AppDetailData.AnalysisMode,
        val appName: String,
        val packageName: String,
        val versionName: String?,
        val versionCode: Long,
        val isSystemApp: Boolean,
        val source: String,
        val apkDirectory: String?,
        val dataDirectory: String?,
        val apkSize: Long,
        val minSdkVersion: Int?,
        val minSdkLabel: String?,
        val targetSdkVersion: Int?,
        val targetSdkLabel: String?,
        val installLocation: String,
        val appInstaller: String?,
        val firstInstallTime: Long?,
        val lastUpdateTime: Long?,
        val signAlgorithms: ImmutableList<String>,
        val activitiesCount: Int,
        val servicesCount: Int,
        val contentProvidersCount: Int,
        val broadcastReceiversCount: Int,
        val definedPermissionsCount: Int,
        val usedPermissionsCount: Int,
        val featuresCount: Int,
        val usedPermissions: ImmutableList<String>,
    ) : AppDetailState

    data object Error : AppDetailState
}
