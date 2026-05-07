package sk.styk.martin.apkanalyzer.core.applist

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

@Singleton
class UsageStatsRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context, private val usageStatsManager: UsageStatsManager, private val appOpsManager: AppOpsManager) :
    UsageStatsRepository,
    DefaultLifecycleObserver {

    private var cache: Map<String, Long>? = null

    override fun onStart(owner: LifecycleOwner) {
        cache = null
    }

    override fun isPermissionGranted(): Boolean {
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun lastUsedTimes(): Map<String, Long> {
        if (!isPermissionGranted()) return emptyMap()
        return cache ?: fetchUsageTimes().also { cache = it }
    }

    private fun fetchUsageTimes(): Map<String, Long> {
        val now = Instant.now()
        val yearAgo = now - 365.days.toJavaDuration()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, yearAgo.toEpochMilli(), now.toEpochMilli())
        return stats
            .groupBy { it.packageName }
            .mapValues { (_, usages) -> usages.maxOf { it.lastTimeUsed } }
    }
}
