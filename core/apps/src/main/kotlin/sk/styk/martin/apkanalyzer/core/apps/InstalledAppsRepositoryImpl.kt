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
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.logger.nextOperationRequest
import sk.styk.martin.apkanalyzer.core.common.logger.operationLogMessage
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceAttributeName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceMetricName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTraceName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.measureStage
import java.io.File
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal const val INSTALLED_APPS = "InstalledApps"
private const val OPERATION_INSTALLED_APPS = "installed_apps"
private const val STAGE_PACKAGE_QUERY = "package_query"
private const val STAGE_APP_MAPPING = "app_mapping"

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

    @Suppress("TooGenericExceptionCaught")
    private val cachedApps = packageChangesObserver.observe()
        .map { InstalledAppsLoadTrigger.PackageChange }
        .onStart { emit(InstalledAppsLoadTrigger.Initial) }
        .mapLatest { trigger ->
            val requestId = nextOperationRequest()
            Logger.d(INSTALLED_APPS, operationLogMessage(OPERATION_INSTALLED_APPS, requestId, event = "started"))
            try {
                val apps = loadAllApps(requestId, trigger)
                Logger.i(
                    INSTALLED_APPS,
                    operationLogMessage(OPERATION_INSTALLED_APPS, requestId, event = "succeeded", context = "count=${apps.size}"),
                )
                apps
            } catch (cancellation: CancellationException) {
                Logger.d(INSTALLED_APPS, operationLogMessage(OPERATION_INSTALLED_APPS, requestId, event = "cancelled"))
                throw cancellation
            } catch (failure: Throwable) {
                Logger.e(INSTALLED_APPS, failure, operationLogMessage(OPERATION_INSTALLED_APPS, requestId, event = "failed"))
                throw failure
            }
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
    private fun loadAllApps(requestId: Long, trigger: InstalledAppsLoadTrigger): List<InstalledApp> {
        val trace = performanceTracker.startTrace(PerformanceTraceName.INSTALLED_APPS_LOAD)
        trace.putAttribute(PerformanceAttributeName.TRIGGER, trigger.attributeValue)
        var outcome = OUTCOME_ERROR
        try {
            Logger.d(INSTALLED_APPS, operationLogMessage(OPERATION_INSTALLED_APPS, requestId, stage = STAGE_PACKAGE_QUERY, event = "started"))
            val packages = trace.measureStage(PerformanceMetricName.PACKAGE_QUERY_US) {
                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
            Logger.d(
                INSTALLED_APPS,
                operationLogMessage(
                    OPERATION_INSTALLED_APPS,
                    requestId,
                    stage = STAGE_PACKAGE_QUERY,
                    event = "succeeded",
                    context = "count=${packages.size}",
                ),
            )

            Logger.d(INSTALLED_APPS, operationLogMessage(OPERATION_INSTALLED_APPS, requestId, stage = STAGE_APP_MAPPING, event = "started"))
            val apps = trace.measureStage(PerformanceMetricName.APP_MAPPING_US) {
                packages.mapNotNull { packageInfo -> packageInfo.applicationInfo?.let { packageInfo.toInstalledApp() } }
            }
            Logger.d(
                INSTALLED_APPS,
                operationLogMessage(
                    OPERATION_INSTALLED_APPS,
                    requestId,
                    stage = STAGE_APP_MAPPING,
                    event = "succeeded",
                    context = "count=${apps.size}",
                ),
            )
            trace.putMetric(PerformanceMetricName.APP_COUNT, apps.size.toLong())
            trace.putAttribute(PerformanceAttributeName.APP_COUNT_BUCKET, appCountBucket(apps.size))
            outcome = OUTCOME_SUCCESS
            return apps
        } catch (cancellation: CancellationException) {
            outcome = OUTCOME_CANCELLED
            throw cancellation
        } finally {
            trace.putAttribute(PerformanceAttributeName.OUTCOME, outcome)
            trace.stop()
        }
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

private const val OUTCOME_SUCCESS = "success"
private const val OUTCOME_ERROR = "error"
private const val OUTCOME_CANCELLED = "cancelled"
private const val TRIGGER_INITIAL = "initial"
private const val TRIGGER_PACKAGE_CHANGE = "package_change"
