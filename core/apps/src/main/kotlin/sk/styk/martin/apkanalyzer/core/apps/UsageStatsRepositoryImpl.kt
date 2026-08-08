package sk.styk.martin.apkanalyzer.core.apps

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
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

@Singleton
internal class UsageStatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsManager: UsageStatsManager,
    private val appOpsManager: AppOpsManager,
    private val dispatcherProvider: DispatcherProvider,
    private val applicationScope: CoroutineScope,
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

    private fun fetchUsageTimes() {
        val hasPermission = checkPermission()
        isPermissionGranted.value = hasPermission
        if (!hasPermission) return

        Logger.d(INSTALLED_APPS, "Load apps last used time")
        lastUsedTimes.value = queryRawUsageStats()
            .groupBy { PackageName(it.packageName) }
            .mapValues { (_, usages) -> Instant.ofEpochMilli(usages.maxOf { it.lastTimeUsed }) }
        Logger.d(INSTALLED_APPS, "Apps last used time loaded")
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
