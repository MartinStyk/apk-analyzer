package sk.styk.martin.apkanalyzer.feature.apps.tv.components

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.api.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

@Immutable
internal data class TvAppItem(
    val packageName: PackageName,
    val applicationName: String,
    val targetSdk: Int,
    val apkSize: AppSize,
    val totalSize: AppSize?,
    val installTime: Instant,
    val lastUpdateTime: Instant,
    val lastUsedTime: Instant?,
)

internal fun InstalledApp.toTvAppItem() = TvAppItem(
    packageName = packageName,
    applicationName = applicationName,
    targetSdk = targetSdk,
    apkSize = apkSize,
    totalSize = totalSize,
    installTime = installTime,
    lastUpdateTime = lastUpdateTime,
    lastUsedTime = lastUsedTime,
)
