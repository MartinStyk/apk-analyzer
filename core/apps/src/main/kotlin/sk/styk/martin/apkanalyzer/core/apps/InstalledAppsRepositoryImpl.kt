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
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import java.io.File
import java.time.Instant
import javax.inject.Inject
import kotlin.time.measureTimedValue

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
    private suspend fun loadAllApps(): List<InstalledApp> = performanceTracker.startCancellableTrace("installed_apps_load") { trace ->
        Logger.d(INSTALLED_APPS, "Installed apps loading started")
        runCatchingCancellable {
            Logger.d(INSTALLED_APPS, "Installed apps package query started")
            val packageQuery = measureTimedValue {
                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
            val packages = packageQuery.value
            trace["package_query_ms"] = packageQuery.duration.inWholeMilliseconds
            Logger.d(INSTALLED_APPS, "Installed apps package query finished: ${packages.size} packages found")

            Logger.d(INSTALLED_APPS, "Installed apps mapping started")
            val apps = packages.mapNotNull { packageInfo ->
                packageInfo.applicationInfo?.let { packageInfo.toInstalledApp() }
            }
            trace["app_count"] = apps.size.toLong()
            Logger.d(INSTALLED_APPS, "Installed apps mapping finished: ${apps.size} apps mapped")
            apps
        }.fold(
            onSuccess = { apps ->
                trace.setOutcome(TraceOutcome.Success)
                Logger.i(INSTALLED_APPS, "Installed apps loading finished: ${apps.size} apps loaded")
                apps
            },
            onFailure = { failure ->
                trace.setOutcome(TraceOutcome.Error)
                Logger.e(INSTALLED_APPS, failure, "Installed apps loading failed")
                throw failure
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
