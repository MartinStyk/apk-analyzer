package sk.styk.martin.apkanalyzer.core.apps.insight

import sk.styk.martin.apkanalyzer.core.apps.AppClassificationThresholds
import sk.styk.martin.apkanalyzer.core.apps.model.AppInfo
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

sealed interface AppDetailBadge {
    data object Large : AppDetailBadge
    data object System : AppDetailBadge
    data object GooglePlay : AppDetailBadge
    data object RecentlyInstalled : AppDetailBadge
    data object RecentlyUpdated : AppDetailBadge
    data object RecentlyUsed : AppDetailBadge
}

object AppDetailBadgeEvaluator {

    fun evaluateRecorded(info: AppInfo): List<AppDetailBadge> = badges(info, now = null)

    fun evaluate(info: AppInfo, now: Instant): List<AppDetailBadge> = badges(info, now)

    private fun badges(info: AppInfo, now: Instant?): List<AppDetailBadge> = buildList {
        val effectiveSize = info.totalSize ?: info.apkSize
        if (effectiveSize >= AppClassificationThresholds.LARGE_SIZE) add(AppDetailBadge.Large)
        if (info.isSystemApp) add(AppDetailBadge.System)
        if (now != null) addRecencyBadges(info, now)
        if (info.source == AppSource.GooglePlay) add(AppDetailBadge.GooglePlay)
    }

    private fun MutableList<AppDetailBadge>.addRecencyBadges(info: AppInfo, now: Instant) {
        info.firstInstallTime?.let { installTime ->
            if (installTime.isAfter(now.minus(AppClassificationThresholds.RECENT_PERIOD))) add(AppDetailBadge.RecentlyInstalled)
        }
        info.lastUpdateTime?.let { updateTime ->
            if (updateTime.isAfter(now.minus(AppClassificationThresholds.RECENT_PERIOD))) add(AppDetailBadge.RecentlyUpdated)
        }
        info.lastUsedTime?.let { lastUsed ->
            if (lastUsed.isAfter(now.minus(AppClassificationThresholds.RECENTLY_USED_DAYS.days.toJavaDuration()))) add(AppDetailBadge.RecentlyUsed)
        }
    }
}
