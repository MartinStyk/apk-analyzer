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
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.TracePermission
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.permission
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import sk.styk.martin.apkanalyzer.core.common.performance.timedSection
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
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
        queryRawUsageStats()
            .filter { it.packageName == packageName.value }
            .maxOfOrNull { it.lastTimeUsed }
            ?.let { Instant.ofEpochMilli(it) }
    } else {
        null
    }

    private suspend fun fetchUsageTimes() {
        performanceTracker.startCancellableTrace("usage_stats_load") {
            runCatchingCancellable {
                val hasPermission = checkPermission()
                permission = if (hasPermission) TracePermission.Granted else TracePermission.Denied
                isPermissionGranted.value = hasPermission

                if (!hasPermission) {
                    Logger.w(TAG, "Usage stats loading degraded: permission missing")
                    TraceOutcome.Degraded
                } else {
                    val rawUsages = timedSection(tag = TAG, operation = "Usage stats query", metric = "usage_stats_query_ms") {
                        queryRawUsageStats()
                    }
                    val usages = rawUsages
                        .groupBy { PackageName(it.packageName) }
                        .mapValues { (_, usages) -> Instant.ofEpochMilli(usages.maxOf { it.lastTimeUsed }) }
                    lastUsedTimes.value = usages
                    Logger.i(TAG, "Usage stats loading finished: ${usages.size} apps loaded")
                    TraceOutcome.Success
                }
            }.fold(
                onSuccess = { outcome = it },
                onFailure = { failure ->
                    outcome = TraceOutcome.Error
                    Logger.e(TAG, failure, "Usage stats loading failed")
                },
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun queryRawUsageStats() = try {
        val now = Instant.now()
        val yearAgo = now - 365.days.toJavaDuration()
        usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, yearAgo.toEpochMilli(), now.toEpochMilli())
    } catch (_: SecurityException) {
        emptyList()
    }

    private fun checkPermission(): Boolean {
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )

        return mode == AppOpsManager.MODE_ALLOWED
    }
}
