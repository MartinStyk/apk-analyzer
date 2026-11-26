package sk.styk.martin.apkanalyzer.feature.appdetail.tv.generalinfo

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.api.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

@Immutable
internal sealed interface TvGeneralInfoState {
    data object Loading : TvGeneralInfoState

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
        val installSourceChain: InstallSourceChain,
        val firstInstallTime: Instant?,
        val lastUpdateTime: Instant?,
        val lastUsedTime: Instant?,
        val apkDirectory: String?,
        val dataDirectory: String?,
        val installLocation: String,
        val apkSize: AppSize,
        val totalSize: AppSize?,
        val nativeLibraryAbis: List<String>,
        val nativeLibraryCount: Int,
        val deviceSupportedAbis: List<String>,
        val isNativeLibraryDeviceIncompatible: Boolean,
        val installedSplitsCount: Int,
    ) : TvGeneralInfoState

    data object Error : TvGeneralInfoState
}
