package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import sk.styk.martin.apkanalyzer.core.applist.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class FilterAppsUseCase @Inject constructor(private val usageStatsRepository: UsageStatsRepository) {

    operator fun invoke(apps: List<InstalledApp>, filter: AppFilterState): List<InstalledApp> {
        if (!filter.isActive) return apps

        val usageStats = if (filter.isUnusedFilterActive) usageStatsRepository.lastUsedTimes() else emptyMap()

        return apps.filter { app ->
            (filter.selectedSources.isEmpty() || app.source in filter.selectedSources) &&
                (filter.selectedSdkVersions.isEmpty() || app.targetSdk in filter.selectedSdkVersions) &&
                (filter.apkSizeRange == null || app.apkSize in filter.apkSizeRange) &&
                matchesInstallTimeRange(app.installTime, filter) &&
                matchesUpdateTimeRange(app.lastUpdateTime, filter) &&
                matchesUnusedFilter(app.packageName, filter, usageStats)
        }
    }

    private fun matchesInstallTimeRange(epochMillis: Long, filter: AppFilterState): Boolean {
        val range = filter.installTimeRange ?: return true
        val instant = Instant.ofEpochMilli(epochMillis)
        return !instant.isBefore(range.start) && !instant.isAfter(range.end)
    }

    private fun matchesUpdateTimeRange(epochMillis: Long, filter: AppFilterState): Boolean {
        val range = filter.updateTimeRange ?: return true
        val instant = Instant.ofEpochMilli(epochMillis)
        return !instant.isBefore(range.start) && !instant.isAfter(range.end)
    }

    private fun matchesUnusedFilter(
        packageName: String,
        filter: AppFilterState,
        usageStats: Map<String, Long>,
    ): Boolean {
        val period = filter.unusedPeriod ?: return true
        if (usageStats.isEmpty()) return true
        val threshold = Instant.now().minusMillis(period.days.days.inWholeMilliseconds)
        val lastUsed = usageStats[packageName]?.let { Instant.ofEpochMilli(it) } ?: return true
        return lastUsed.isBefore(threshold)
    }
}
