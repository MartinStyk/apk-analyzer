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
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

private const val TAG = "UsageStatsRepositoryImpl"

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

    private suspend fun fetchUsageTimes() {
        performanceTracker.startCancellableTrace(TRACE_USAGE_STATS_LOAD) { trace ->
            trace[ATTRIBUTE_TRIGGER] = TRIGGER_LIFECYCLE_START
            val result = runCatchingCancellable {
                val permissionCheck = measureTimedValue { checkPermission() }
                trace[METRIC_PERMISSION_CHECK_US] = permissionCheck.duration.inWholeMicroseconds
                trace[ATTRIBUTE_PERMISSION] = if (permissionCheck.value) PERMISSION_GRANTED else PERMISSION_DENIED
                isPermissionGranted.value = permissionCheck.value

                if (!permissionCheck.value) {
                    trace[METRIC_LOADED_COUNT] = 0L
                    Logger.w(TAG, "Usage stats loading degraded: permission missing")
                    OUTCOME_DEGRADED
                } else {
                    Logger.d(TAG, "Usage stats loading started")
                    val usageQuery = measureTimedValue {
                        queryRawUsageStats(detectPermissionRace = true)
                    }
                    trace[METRIC_USAGE_QUERY_US] = usageQuery.duration.inWholeMicroseconds
                    val usageMapping = measureTimedValue {
                        usageQuery.value.usages
                            .groupBy { PackageName(it.packageName) }
                            .mapValues { (_, usages) -> Instant.ofEpochMilli(usages.maxOf { it.lastTimeUsed }) }
                    }
                    trace[METRIC_USAGE_MAPPING_US] = usageMapping.duration.inWholeMicroseconds
                    trace[METRIC_LOADED_COUNT] = usageMapping.value.size.toLong()
                    lastUsedTimes.value = usageMapping.value
                    if (usageQuery.value.permissionRace) {
                        trace[ATTRIBUTE_PERMISSION] = PERMISSION_DENIED
                    }
                    Logger.i(TAG, "Usage stats loading finished: ${usageMapping.value.size} apps loaded")
                    if (usageQuery.value.permissionRace) OUTCOME_DEGRADED else OUTCOME_SUCCESS
                }
            }

            result.fold(
                onSuccess = { outcome -> trace[ATTRIBUTE_OUTCOME] = outcome },
                onFailure = { failure ->
                    trace[ATTRIBUTE_OUTCOME] = OUTCOME_ERROR
                    Logger.e(TAG, failure, "Usage stats loading failed")
                    throw failure
                },
            )
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

private const val TRACE_USAGE_STATS_LOAD = "usage_stats_load"
private const val METRIC_PERMISSION_CHECK_US = "permission_check_us"
private const val METRIC_USAGE_QUERY_US = "usage_query_us"
private const val METRIC_USAGE_MAPPING_US = "usage_mapping_us"
private const val METRIC_LOADED_COUNT = "loaded_count"
private const val ATTRIBUTE_OUTCOME = "outcome"
private const val ATTRIBUTE_PERMISSION = "permission"
private const val ATTRIBUTE_TRIGGER = "trigger"
private const val OUTCOME_SUCCESS = "success"
private const val OUTCOME_DEGRADED = "degraded"
private const val OUTCOME_ERROR = "error"
private const val PERMISSION_GRANTED = "granted"
private const val PERMISSION_DENIED = "denied"
private const val TRIGGER_LIFECYCLE_START = "lifecycle_start"
