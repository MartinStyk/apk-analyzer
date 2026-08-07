package sk.styk.martin.apkanalyzer.core.apps.model

import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

data class InstalledApp(
    val packageName: PackageName,
    val applicationName: String,
    val isSystemApp: Boolean,
    val version: Long,
    val source: AppSource,
    val targetSdk: Int,
    val minSdk: Int,
    val apkSize: AppSize,
    val versionName: String?,
    val installTime: Instant,
    val lastUpdateTime: Instant,
    val requestedPermissions: List<String> = emptyList(),
    val totalSize: AppSize? = null,
    val lastUsedTime: Instant? = null,
)
