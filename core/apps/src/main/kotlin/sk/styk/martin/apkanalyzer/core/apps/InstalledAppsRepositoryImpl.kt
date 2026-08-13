package sk.styk.martin.apkanalyzer.core.apps

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
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
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceResolver
import sk.styk.martin.apkanalyzer.core.apps.installsource.isSystemInstalledApp
import sk.styk.martin.apkanalyzer.core.apps.installsource.resolveAppInstallSource
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.model.resolveAppCategory
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.appCount
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import sk.styk.martin.apkanalyzer.core.common.performance.timedSection
import java.io.File
import java.time.Instant
import javax.inject.Inject

internal const val INSTALLED_APPS = "InstalledApps"

internal class InstalledAppsRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val installSourceResolver: InstallSourceResolver,
    packageChangesObserver: PackageChangesObserver,
    dispatcherProvider: DispatcherProvider,
    storageStatsRepository: StorageStatsRepository,
    usageStatsRepository: UsageStatsRepository,
    appScope: CoroutineScope,
    private val performanceTracker: PerformanceTracker,
) : InstalledAppsRepository {
    private val cachedApps = packageChangesObserver.observe()
        .onStart { emit(Unit) }
        .mapLatest { loadAllApps() }
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
                    }
                } else {
                    apps
                }
            }
        }
        .flowOn(dispatcherProvider.io())
        .shareIn(appScope, SharingStarted.Eagerly, replay = 1)

    override fun apps(): Flow<List<InstalledApp>> = cachedApps

    @SuppressLint("QueryPermissionsNeeded")
    private suspend fun loadAllApps(): List<InstalledApp> = performanceTracker.startCancellableTrace("installed_apps_load") {
        Logger.i(INSTALLED_APPS, "Installed apps loading started")
        runCatchingCancellable {
            val packages = timedSection(tag = INSTALLED_APPS, operation = "Installed apps package query", metric = "package_query_ms") {
                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

            val apps = timedSection(tag = INSTALLED_APPS, operation = "Installed apps mapping", metric = "apps_mapping_ms") {
                packages.mapNotNull { packageInfo -> packageInfo.applicationInfo?.let { packageInfo.toInstalledApp() } }
            }
            appCount = apps.size
            apps
        }.fold(
            onSuccess = { apps ->
                outcome = TraceOutcome.Success
                Logger.i(INSTALLED_APPS, "Installed apps loading finished: ${apps.size} apps loaded")
                apps
            },
            onFailure = { failure ->
                outcome = TraceOutcome.Error
                Logger.e(INSTALLED_APPS, failure, "Installed apps loading failed")
                emptyList()
            },
        )
    }

    private fun PackageInfo.toInstalledApp(): InstalledApp {
        val appInfo = applicationInfo
        val installSourceChain = installSourceResolver.resolve(this)
        val isSystemApp = isSystemInstalledApp(this)
        return InstalledApp(
            packageName = PackageName(packageName),
            applicationName = appInfo?.loadLabel(packageManager)?.toString() ?: packageName,
            isSystemApp = isSystemApp,
            installSourceChain = installSourceChain,
            version = longVersionCode,
            source = resolveAppInstallSource(installSourceChain, isSystemApp),
            targetSdk = appInfo?.targetSdkVersion ?: 0,
            minSdk = appInfo?.minSdkVersion ?: 0,
            apkSize = (appInfo?.sourceDir?.let { File(it).length() } ?: 0L).bytes,
            versionName = versionName,
            installTime = Instant.ofEpochMilli(firstInstallTime),
            lastUpdateTime = Instant.ofEpochMilli(lastUpdateTime),
            requestedPermissions = requestedPermissions?.toList().orEmpty(),
            sharedUserId = sharedUserId,
            category = resolveAppCategory(appInfo?.category ?: ApplicationInfo.CATEGORY_UNDEFINED),
        )
    }
}
