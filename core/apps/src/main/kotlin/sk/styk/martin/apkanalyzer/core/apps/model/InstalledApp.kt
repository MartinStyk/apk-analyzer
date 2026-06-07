package sk.styk.martin.apkanalyzer.core.apps.model

import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import java.time.Instant

data class InstalledApp(
    val packageName: String,
    val applicationName: String,
    val isSystemApp: Boolean,
    val version: Long,
    val source: AppSource,
    val targetSdk: Int,
    val apkSize: AppSize,
    val versionName: String?,
    val installTime: Instant,
    val lastUpdateTime: Instant,
    val requestedPermissions: List<String> = emptyList(),
    val totalSize: AppSize? = null,
    val lastUsedTime: Instant? = null,
)
