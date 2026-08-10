package sk.styk.martin.apkanalyzer.core.apps.storagestats

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.measureTimedValue

private const val TAG = "StorageStatsRepositoryImpl"

@Singleton
internal class StorageStatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageStatsManager: StorageStatsManager,
    private val appOpsManager: AppOpsManager,
    private val dispatcherProvider: DispatcherProvider,
    private val applicationScope: CoroutineScope,
    private val performanceTracker: PerformanceTracker,
) : StorageStatsRepository,
    DefaultLifecycleObserver {

    private var packageNames: List<PackageName> = emptyList()

    final override val isPermissionGranted: StateFlow<Boolean>
        field = MutableStateFlow(checkPermission())

    final override val totalSizes: StateFlow<Map<PackageName, AppSize>>
        field = MutableStateFlow<Map<PackageName, AppSize>>(emptyMap())

    override fun onStart(owner: LifecycleOwner) {
        applicationScope.launch(dispatcherProvider.io()) {
            fetchTotalSizes(packageNames, TRIGGER_LIFECYCLE_START)
        }
    }

    private fun checkPermission(): Boolean {
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun requestTotalSizes(packageNames: List<PackageName>) {
        this.packageNames = packageNames
        applicationScope.launch(dispatcherProvider.io()) {
            fetchTotalSizes(packageNames, TRIGGER_INSTALLED_APPS)
        }
    }

    override suspend fun queryTotalSize(packageName: PackageName): AppSize? = totalSizes.value[packageName] ?: if (checkPermission()) {
        when (val result = queryPackageSize(UserHandle.getUserHandleForUid(Process.myUid()), packageName)) {
            is SizeQueryResult.Success -> result.size

            SizeQueryResult.UninstallRace -> null

            SizeQueryResult.PermissionRace -> null

            is SizeQueryResult.Failure -> {
                Logger.w(TAG, result.error, "Storage stats loading degraded: query failed for ${packageName.value}")
                null
            }
        }
    } else {
        null
    }

    private suspend fun fetchTotalSizes(packageNames: List<PackageName>, trigger: String) {
        performanceTracker.startTrace(TRACE_STORAGE_STATS_LOAD).use { trace ->
            trace.putAttribute(ATTRIBUTE_TRIGGER, trigger)
            trace.putMetric(METRIC_REQUESTED_COUNT, packageNames.size.toLong())
            val result = try {
                runCatchingCancellable {
                    val permissionCheck = measureTimedValue { checkPermission() }
                    trace.putMetric(METRIC_PERMISSION_CHECK_US, permissionCheck.duration.inWholeMicroseconds)
                    trace.putAttribute(
                        ATTRIBUTE_PERMISSION,
                        if (permissionCheck.value) PERMISSION_GRANTED else PERMISSION_DENIED,
                    )
                    isPermissionGranted.value = permissionCheck.value

                    if (!permissionCheck.value) {
                        trace.putMetric(METRIC_LOADED_COUNT, 0)
                        Logger.w(TAG, "Storage stats loading degraded: permission missing")
                        OUTCOME_DEGRADED
                    } else {
                        Logger.d(TAG, "Storage stats loading started: ${packageNames.size} apps requested")
                        val user = UserHandle.getUserHandleForUid(Process.myUid())
                        var uninstallRaceCount = 0
                        var permissionRaceCount = 0
                        var queryFailureCount = 0
                        var lastQueryFailure: IOException? = null
                        val statsQuery = measureTimedValue {
                            packageNames.mapNotNull { packageName ->
                                when (val queryResult = queryPackageSize(user, packageName)) {
                                    is SizeQueryResult.Success -> packageName to queryResult.size

                                    SizeQueryResult.UninstallRace -> {
                                        uninstallRaceCount++
                                        null
                                    }

                                    SizeQueryResult.PermissionRace -> {
                                        permissionRaceCount++
                                        null
                                    }

                                    is SizeQueryResult.Failure -> {
                                        queryFailureCount++
                                        lastQueryFailure = queryResult.error
                                        null
                                    }
                                }
                            }.toMap()
                        }
                        trace.putMetric(METRIC_STATS_QUERY_US, statsQuery.duration.inWholeMicroseconds)
                        trace.putMetric(METRIC_LOADED_COUNT, statsQuery.value.size.toLong())
                        totalSizes.value = statsQuery.value

                        if (uninstallRaceCount > 0) {
                            Logger.w(TAG, "Storage stats loading degraded: $uninstallRaceCount uninstalled apps skipped")
                        }
                        if (permissionRaceCount > 0) {
                            Logger.w(
                                TAG,
                                "Storage stats loading degraded: $permissionRaceCount apps skipped after permission changed",
                            )
                        }
                        lastQueryFailure?.let {
                            Logger.w(TAG, it, "Storage stats loading degraded: $queryFailureCount app queries failed")
                        }
                        Logger.i(TAG, "Storage stats loading finished: ${statsQuery.value.size} apps loaded")
                        if (uninstallRaceCount > 0 || permissionRaceCount > 0 || queryFailureCount > 0) {
                            OUTCOME_DEGRADED
                        } else {
                            OUTCOME_SUCCESS
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                trace.putAttribute(ATTRIBUTE_OUTCOME, OUTCOME_CANCELLED)
                throw cancellation
            }

            result.fold(
                onSuccess = { outcome -> trace.putAttribute(ATTRIBUTE_OUTCOME, outcome) },
                onFailure = { failure ->
                    trace.putAttribute(ATTRIBUTE_OUTCOME, OUTCOME_ERROR)
                    Logger.e(TAG, failure, "Storage stats loading failed")
                    throw failure
                },
            )
        }
    }

    private fun queryPackageSize(user: UserHandle, packageName: PackageName): SizeQueryResult = try {
        val stats = storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, packageName.value, user)
        SizeQueryResult.Success(stats.appBytes.bytes + stats.dataBytes.bytes + stats.cacheBytes.bytes)
    } catch (_: PackageManager.NameNotFoundException) {
        SizeQueryResult.UninstallRace
    } catch (e: IOException) {
        SizeQueryResult.Failure(e)
    } catch (_: SecurityException) {
        SizeQueryResult.PermissionRace
    }

    private sealed interface SizeQueryResult {
        data class Success(val size: AppSize) : SizeQueryResult
        data object UninstallRace : SizeQueryResult
        data object PermissionRace : SizeQueryResult
        data class Failure(val error: IOException) : SizeQueryResult
    }
}

private const val TRACE_STORAGE_STATS_LOAD = "storage_stats_load"
private const val METRIC_PERMISSION_CHECK_US = "permission_check_us"
private const val METRIC_STATS_QUERY_US = "stats_query_us"
private const val METRIC_REQUESTED_COUNT = "requested_count"
private const val METRIC_LOADED_COUNT = "loaded_count"
private const val ATTRIBUTE_OUTCOME = "outcome"
private const val ATTRIBUTE_PERMISSION = "permission"
private const val ATTRIBUTE_TRIGGER = "trigger"
private const val OUTCOME_SUCCESS = "success"
private const val OUTCOME_DEGRADED = "degraded"
private const val OUTCOME_ERROR = "error"
private const val OUTCOME_CANCELLED = "cancelled"
private const val PERMISSION_GRANTED = "granted"
private const val PERMISSION_DENIED = "denied"
private const val TRIGGER_LIFECYCLE_START = "lifecycle_start"
private const val TRIGGER_INSTALLED_APPS = "installed_apps"
