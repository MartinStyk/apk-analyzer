package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

class FilterAppsUseCase @Inject constructor() {

    operator fun invoke(apps: List<InstalledApp>, filter: AppFilterState): List<InstalledApp> {
        if (!filter.isActive) return apps
        val predicates = filter.toPredicates()
        return apps.filter { app -> predicates.all { it(app) } }
    }

    private fun AppFilterState.toPredicates(): List<(InstalledApp) -> Boolean> = buildList {
        if (selectedSources.isNotEmpty()) {
            add { it.source in selectedSources }
        }
        if (selectedSdkVersions.isNotEmpty()) {
            add { it.targetSdk in selectedSdkVersions }
        }
        apkSizeRange?.let { range ->
            add { it.apkSize in range }
        }
        totalSizeRange?.let { range ->
            add { app -> app.totalSize?.let { it in range } ?: true }
        }
        installTimeRange?.let { range ->
            add { !it.installTime.isBefore(range.start) && !it.installTime.isAfter(range.end) }
        }
        updateTimeRange?.let { range ->
            add { !it.lastUpdateTime.isBefore(range.start) && !it.lastUpdateTime.isAfter(range.end) }
        }
        unusedPeriod?.let { period ->
            add { app ->
                val lastUsed = app.lastUsedTime ?: return@add true
                val threshold = Instant.now().minusMillis(period.days.days.inWholeMilliseconds)
                lastUsed.isBefore(threshold)
            }
        }
        recentlyUsedDays?.let { recentDays ->
            add { app ->
                val lastUsed = app.lastUsedTime ?: return@add false
                val threshold = Instant.now().minus(recentDays.days.toJavaDuration())
                lastUsed.isAfter(threshold)
            }
        }
    }
}
