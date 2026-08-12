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
import kotlinx.coroutines.flow.map
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
        .map { InstalledAppsLoadTrigger.PackageChange }
        .onStart { emit(InstalledAppsLoadTrigger.Initial) }
        .mapLatest { trigger ->
            Logger.d(INSTALLED_APPS, "Installed apps loading started")
            loadAllApps(trigger)
        }
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
    private suspend fun loadAllApps(trigger: InstalledAppsLoadTrigger): List<InstalledApp> =
        performanceTracker.startCancellableTrace(TRACE_INSTALLED_APPS_LOAD) { trace ->
            trace[ATTRIBUTE_TRIGGER] = trigger.attributeValue
            val result = runCatchingCancellable {
                Logger.d(INSTALLED_APPS, "Installed apps package query started")
                val packageQuery = measureTimedValue {
                    packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                }
                trace[METRIC_PACKAGE_QUERY_US] = packageQuery.duration.inWholeMicroseconds
                Logger.d(
                    INSTALLED_APPS,
                    "Installed apps package query finished: ${packageQuery.value.size} packages found",
                )

                Logger.d(INSTALLED_APPS, "Installed apps mapping started")
                val appMapping = measureTimedValue {
                    packageQuery.value.mapNotNull { packageInfo ->
                        packageInfo.applicationInfo?.let { packageInfo.toInstalledApp() }
                    }
                }
                trace[METRIC_APP_MAPPING_US] = appMapping.duration.inWholeMicroseconds
                trace[METRIC_APP_COUNT] = appMapping.value.size.toLong()
                trace[ATTRIBUTE_APP_COUNT_BUCKET] = appCountBucket(appMapping.value.size)
                Logger.d(
                    INSTALLED_APPS,
                    "Installed apps mapping finished: ${appMapping.value.size} apps mapped",
                )
                appMapping.value
            }

            result.fold(
                onSuccess = { apps ->
                    trace[ATTRIBUTE_OUTCOME] = OUTCOME_SUCCESS
                    Logger.i(INSTALLED_APPS, "Installed apps loading finished: ${apps.size} apps loaded")
                    apps
                },
                onFailure = { failure ->
                    trace[ATTRIBUTE_OUTCOME] = OUTCOME_ERROR
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

private enum class InstalledAppsLoadTrigger(val attributeValue: String) {
    Initial(TRIGGER_INITIAL),
    PackageChange(TRIGGER_PACKAGE_CHANGE),
}

private fun appCountBucket(appCount: Int): String = when (appCount) {
    in 0..APP_COUNT_SMALL_MAX -> "0_49"
    in APP_COUNT_MEDIUM_MIN..APP_COUNT_MEDIUM_MAX -> "50_99"
    in APP_COUNT_LARGE_MIN..APP_COUNT_LARGE_MAX -> "100_199"
    in APP_COUNT_VERY_LARGE_MIN..APP_COUNT_VERY_LARGE_MAX -> "200_399"
    else -> "400_plus"
}

private const val TRACE_INSTALLED_APPS_LOAD = "installed_apps_load"
private const val METRIC_PACKAGE_QUERY_US = "package_query_us"
private const val METRIC_APP_MAPPING_US = "app_mapping_us"
private const val METRIC_APP_COUNT = "app_count"
private const val ATTRIBUTE_OUTCOME = "outcome"
private const val ATTRIBUTE_TRIGGER = "trigger"
private const val ATTRIBUTE_APP_COUNT_BUCKET = "app_count_bucket"
private const val OUTCOME_SUCCESS = "success"
private const val OUTCOME_ERROR = "error"
private const val TRIGGER_INITIAL = "initial"
private const val TRIGGER_PACKAGE_CHANGE = "package_change"
private const val APP_COUNT_SMALL_MAX = 49
private const val APP_COUNT_MEDIUM_MIN = 50
private const val APP_COUNT_MEDIUM_MAX = 99
private const val APP_COUNT_LARGE_MIN = 100
private const val APP_COUNT_LARGE_MAX = 199
private const val APP_COUNT_VERY_LARGE_MIN = 200
private const val APP_COUNT_VERY_LARGE_MAX = 399
