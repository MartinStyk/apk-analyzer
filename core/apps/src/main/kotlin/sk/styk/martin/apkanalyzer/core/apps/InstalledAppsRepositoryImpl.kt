package sk.styk.martin.apkanalyzer.core.apps

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import sk.styk.martin.apkanalyzer.core.apps.analysis.InstallSourceResolver
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.File
import java.time.Instant
import javax.inject.Inject

internal const val INSTALLED_APPS = "InstalledApps"

internal class InstalledAppsRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val installSourceResolver: InstallSourceResolver,
    private val packageChangesObserver: PackageChangesObserver,
    private val dispatcherProvider: DispatcherProvider,
    private val storageStatsRepository: StorageStatsRepository,
    private val usageStatsRepository: UsageStatsRepository,
    appScope: CoroutineScope,
) : InstalledAppsRepository {

    private val cachedApps = packageChangesObserver.observe()
        .onStart { emit(Unit) }
        .onEach { Logger.i(INSTALLED_APPS, "Load apps") }
        .mapLatest { loadAllApps() }
        .onEach { Logger.i(INSTALLED_APPS, "Apps loaded: ${it.size}") }
        .onEach { apps -> storageStatsRepository.requestTotalSizes(apps.map { it.packageName }) }
        .flatMapLatest { apps ->
            combine(
                storageStatsRepository.totalSizes,
                usageStatsRepository.lastUsedTimes,
            ) { totalSizes, lastUsedTimes ->
                if (totalSizes.isNotEmpty() || lastUsedTimes.isNotEmpty()) {
                    apps.map { app ->
                        app.copy(
                            totalSize = totalSizes[app.packageName],
                            lastUsedTime = lastUsedTimes[app.packageName],
                        )
                    }.also { Logger.i(INSTALLED_APPS, "Enriched ${it.size} apps") }
                } else {
                    apps
                }
            }
        }
        .flowOn(dispatcherProvider.io())
        .shareIn(appScope, SharingStarted.Eagerly, replay = 1)

    override fun apps(): Flow<List<InstalledApp>> = cachedApps

    private fun loadAllApps(): List<InstalledApp> = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS).mapNotNull { packageInfo ->
        packageInfo.applicationInfo?.let { packageInfo.toInstalledApp() }
    }

    private fun PackageInfo.toInstalledApp(): InstalledApp {
        val appInfo = applicationInfo
        return InstalledApp(
            packageName = PackageName(packageName),
            applicationName = appInfo?.loadLabel(packageManager)?.toString() ?: packageName,
            isSystemApp = installSourceResolver.isSystemInstalledApp(this),
            version = longVersionCode,
            source = installSourceResolver.getAppInstallSource(this),
            targetSdk = appInfo?.targetSdkVersion ?: 0,
            minSdk = appInfo?.minSdkVersion ?: 0,
            apkSize = (appInfo?.sourceDir?.let { File(it).length() } ?: 0L).bytes,
            versionName = versionName,
            installTime = Instant.ofEpochMilli(firstInstallTime),
            lastUpdateTime = Instant.ofEpochMilli(lastUpdateTime),
            requestedPermissions = requestedPermissions?.toList() ?: emptyList(),
        )
    }
}
