package sk.styk.martin.apkanalyzer.core.apps.model

import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.time.Instant

data class AppInfo(
    val packageName: PackageName,
    val applicationName: String,
    val processName: String? = null,
    val versionName: String? = null,
    val versionCode: Long = 0,
    val isSystemApp: Boolean = false,
    val uid: Int? = null,
    val description: String? = null,
    val source: AppSource = AppSource.Unknown,
    val apkDirectory: String? = null,
    val dataDirectory: String? = null,
    val installLocation: InstallLocation,
    val appInstaller: PackageName? = null,
    val apkSize: AppSize = 0.bytes,
    val firstInstallTime: Instant? = null,
    val lastUpdateTime: Instant? = null,
    val minSdkVersion: Int? = null,
    val minSdkLabel: String? = null,
    val targetSdkVersion: Int? = null,
    val targetSdkLabel: String? = null,
    val totalSize: AppSize? = null,
    val lastUsedTime: Instant? = null,
)
