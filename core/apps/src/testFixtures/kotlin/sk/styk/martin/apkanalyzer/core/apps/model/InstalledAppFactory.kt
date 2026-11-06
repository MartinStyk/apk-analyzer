package sk.styk.martin.apkanalyzer.core.apps.model

import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Instant

fun anApp(
    name: String,
    packageName: String = "com.example.${name.lowercase()}",
    versionName: String = "1.0",
    source: AppSource = AppSource.GooglePlay,
    targetSdk: Int = 36,
    requestedPermissions: List<String> = emptyList(),
    totalSize: AppSize? = 10.megabytes,
    apkSize: AppSize = 5.megabytes,
    installTime: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    lastUpdateTime: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    lastUsedTime: Instant? = Instant.parse("2026-03-01T00:00:00Z"),
): InstalledApp = InstalledApp(
    packageName = PackageName(packageName),
    applicationName = name,
    isSystemApp = false,
    installSourceChain = InstallSourceChain(),
    version = 1,
    source = source,
    targetSdk = targetSdk,
    minSdk = 28,
    apkSize = apkSize,
    versionName = versionName,
    installTime = installTime,
    lastUpdateTime = lastUpdateTime,
    requestedPermissions = requestedPermissions,
    totalSize = totalSize,
    lastUsedTime = lastUsedTime,
)
