package sk.styk.martin.apkanalyzer.core.apps.usagestats

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.logger.nextOperationRequest
import sk.styk.martin.apkanalyzer.core.common.logger.operationLogMessage
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceAttributeName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceMetricName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTraceName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.measureStage
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

private const val TAG = "UsageStatsRepositoryImpl"
private const val OPERATION = "usage_stats"

@Singleton
internal class UsageStatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsManager: UsageStatsManager,
    private val appOpsManager: AppOpsManager,
    private val dispatcherProvider: DispatcherProvider,
    private val applicationScope: CoroutineScope,
    private val performanceTracker: PerformanceTracker,
) : UsageStatsRepository,
    DefaultLifecycleObserver {

    final override val isPermissionGranted: StateFlow<Boolean>
        field = MutableStateFlow(checkPermission())

    final override val lastUsedTimes: StateFlow<Map<PackageName, Instant>>
        field = MutableStateFlow<Map<PackageName, Instant>>(emptyMap())

    override fun onStart(owner: LifecycleOwner) {
        applicationScope.launch(dispatcherProvider.default()) {
            fetchUsageTimes()
        }
    }

    override suspend fun queryLastUsedTime(packageName: PackageName): Instant? = lastUsedTimes.value[packageName] ?: if (checkPermission()) {
        queryRawUsageStats().usages
            .filter { it.packageName == packageName.value }
            .maxOfOrNull { it.lastTimeUsed }
            ?.let { Instant.ofEpochMilli(it) }
    } else {
        null
    }

    private fun fetchUsageTimes() {
        val requestId = nextOperationRequest()
        val trace = performanceTracker.startTrace(PerformanceTraceName.USAGE_STATS_LOAD)
        trace.putAttribute(PerformanceAttributeName.TRIGGER, TRIGGER_LIFECYCLE_START)
        var outcome = OUTCOME_ERROR
        try {
            val hasPermission = trace.measureStage(PerformanceMetricName.PERMISSION_CHECK_US) {
                checkPermission()
            }
            trace.putAttribute(PerformanceAttributeName.PERMISSION, if (hasPermission) PERMISSION_GRANTED else PERMISSION_DENIED)
            isPermissionGranted.value = hasPermission
            if (!hasPermission) {
                trace.putMetric(PerformanceMetricName.LOADED_COUNT, 0)
                Logger.w(TAG, operationLogMessage(OPERATION, requestId, event = "degraded", context = "reason=permission_missing"))
                outcome = OUTCOME_DEGRADED
                return
            }

            Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "started"))
            val queryResult = trace.measureStage(PerformanceMetricName.USAGE_QUERY_US) {
                queryRawUsageStats(detectPermissionRace = true)
            }
            val usages = trace.measureStage(PerformanceMetricName.USAGE_MAPPING_US) {
                queryResult.usages
                    .groupBy { PackageName(it.packageName) }
                    .mapValues { (_, usages) -> Instant.ofEpochMilli(usages.maxOf { it.lastTimeUsed }) }
            }
            lastUsedTimes.value = usages
            trace.putMetric(PerformanceMetricName.LOADED_COUNT, usages.size.toLong())
            if (queryResult.permissionRace) {
                trace.putAttribute(PerformanceAttributeName.PERMISSION, PERMISSION_DENIED)
            }
            Logger.i(TAG, operationLogMessage(OPERATION, requestId, event = "succeeded", context = "loaded_count=${usages.size}"))
            outcome = if (queryResult.permissionRace) OUTCOME_DEGRADED else OUTCOME_SUCCESS
        } catch (cancellation: CancellationException) {
            outcome = OUTCOME_CANCELLED
            throw cancellation
        } finally {
            trace.putAttribute(PerformanceAttributeName.OUTCOME, outcome)
            trace.stop()
        }
    }

    @SuppressLint("MissingPermission")
    private fun queryRawUsageStats(detectPermissionRace: Boolean = false): UsageStatsQueryResult = try {
        val now = Instant.now()
        val yearAgo = now - 365.days.toJavaDuration()
        val usages = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, yearAgo.toEpochMilli(), now.toEpochMilli())
        UsageStatsQueryResult(
            usages = usages,
            permissionRace = detectPermissionRace && usages.isEmpty() && !checkPermission(),
        )
    } catch (_: SecurityException) {
        UsageStatsQueryResult(usages = emptyList(), permissionRace = true)
    }

    private fun checkPermission(): Boolean {
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )

        return mode == AppOpsManager.MODE_ALLOWED
    }

    private data class UsageStatsQueryResult(val usages: List<android.app.usage.UsageStats>, val permissionRace: Boolean)
}

private const val OUTCOME_SUCCESS = "success"
private const val OUTCOME_DEGRADED = "degraded"
private const val OUTCOME_ERROR = "error"
private const val OUTCOME_CANCELLED = "cancelled"
private const val PERMISSION_GRANTED = "granted"
private const val PERMISSION_DENIED = "denied"
private const val TRIGGER_LIFECYCLE_START = "lifecycle_start"
