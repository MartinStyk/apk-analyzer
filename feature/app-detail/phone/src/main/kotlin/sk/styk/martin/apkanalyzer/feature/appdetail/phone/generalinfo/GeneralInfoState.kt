package sk.styk.martin.apkanalyzer.feature.appdetail.phone.generalinfo

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.RecordedVersionFailure
import java.time.Instant

@Immutable
internal sealed interface GeneralInfoState {
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
        val nativeLibraryNames: List<String>,
        val deviceSupportedAbis: List<String>,
        val isNativeLibraryDeviceIncompatible: Boolean,
        val isNativeLibrariesRecorded: Boolean = true,
        val installedSplitsCount: Int,
    ) : GeneralInfoState

    data object Error : GeneralInfoState

    @Immutable
    data class RecordedVersionError(val failure: RecordedVersionFailure) : GeneralInfoState
}
