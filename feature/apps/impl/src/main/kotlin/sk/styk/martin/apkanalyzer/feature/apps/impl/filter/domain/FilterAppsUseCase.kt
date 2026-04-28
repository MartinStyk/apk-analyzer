package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp
import java.time.Instant
import javax.inject.Inject

class FilterAppsUseCase @Inject constructor() {

    operator fun invoke(apps: List<InstalledApp>, filter: AppFilterState): List<InstalledApp> {
        if (!filter.isActive) return apps

        return apps.filter { app ->
            (filter.selectedSources.isEmpty() || app.source in filter.selectedSources) &&
                (filter.selectedSdkVersions.isEmpty() || app.targetSdk in filter.selectedSdkVersions) &&
                (filter.apkSizeRange == null || app.apkSize in filter.apkSizeRange) &&
                matchesInstallTimeRange(app.installTime, filter) &&
                matchesUpdateTimeRange(app.lastUpdateTime, filter)
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
}
