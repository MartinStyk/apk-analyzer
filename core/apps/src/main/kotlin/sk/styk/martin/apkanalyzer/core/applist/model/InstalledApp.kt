package sk.styk.martin.apkanalyzer.core.applist.model

import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

data class InstalledApp(
    val packageName: String,
    val applicationName: String,
    val isSystemApp: Boolean,
    val version: Long,
    val source: AppSource,
    val targetSdk: Int,
    val apkSize: AppSize,
    val versionName: String?,
    val installTime: Long,
    val lastUpdateTime: Long,
)
