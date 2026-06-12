package sk.styk.martin.apkanalyzer.core.apps.model

import sk.styk.martin.apkanalyzer.core.common.model.AppSource

data class AppInfo(
    val packageName: String,
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
    val appInstaller: String? = null,
    val apkSize: Long = 0,
    val firstInstallTime: Long? = null,
    val lastUpdateTime: Long? = null,
    val minSdkVersion: Int? = null,
    val minSdkLabel: String? = null,
    val targetSdkVersion: Int? = null,
    val targetSdkLabel: String? = null,
)
