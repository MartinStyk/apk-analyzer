package sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight

import sk.styk.martin.apkanalyzer.core.apps.AppClassificationThresholds
import sk.styk.martin.apkanalyzer.core.apps.model.AppInfo
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.isOnDevice
import java.time.Instant
import javax.inject.Inject
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

class AppDetailBadgeEvaluator @Inject constructor() {

    fun evaluate(info: AppInfo, input: AppDetailInput): List<AppDetailBadge> = buildList {
        val effectiveSize = info.totalSize ?: info.apkSize
        if (effectiveSize >= AppClassificationThresholds.LARGE_SIZE) add(AppDetailBadge.Large)
        if (info.isSystemApp) add(AppDetailBadge.System)
        if (input.isOnDevice) addRecencyBadges(info, Instant.now())
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
