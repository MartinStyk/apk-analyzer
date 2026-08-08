package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain

import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
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

    private fun AppFilterState.toPredicates(): List<(InstalledApp) -> Boolean> = listOfNotNull(
        sourcePredicate(),
        sdkPredicate(),
        apkSizeRange?.let { range -> { app: InstalledApp -> app.apkSize in range } },
        totalSizeRange?.let { range -> { app: InstalledApp -> app.totalSize?.let { it in range } ?: true } },
        installTimeRange?.let { range ->
            { app: InstalledApp -> !app.installTime.isBefore(range.start) && !app.installTime.isAfter(range.end) }
        },
        updateTimeRange?.let { range ->
            { app: InstalledApp -> !app.lastUpdateTime.isBefore(range.start) && !app.lastUpdateTime.isAfter(range.end) }
        },
        unusedPredicate(),
        recentlyUsedPredicate(),
        permissionPredicate(),
    )

    private fun AppFilterState.sourcePredicate(): ((InstalledApp) -> Boolean)? = selectedSources.takeIf { it.isNotEmpty() }?.let { sources ->
        { app -> app.source in sources }
    }

    private fun AppFilterState.sdkPredicate(): ((InstalledApp) -> Boolean)? = selectedSdkVersions.takeIf { it.isNotEmpty() }?.let { versions ->
        { app -> app.targetSdk in versions }
    }

    private fun AppFilterState.unusedPredicate(): ((InstalledApp) -> Boolean)? {
        val period = unusedPeriod ?: return null
        return { app ->
            val lastUsed = app.lastUsedTime
            lastUsed == null || lastUsed.isBefore(Instant.now().minusMillis(period.days.days.inWholeMilliseconds))
        }
    }

    private fun AppFilterState.recentlyUsedPredicate(): ((InstalledApp) -> Boolean)? {
        val recentDays = recentlyUsedDays ?: return null
        return { app ->
            val lastUsed = app.lastUsedTime
            lastUsed != null && lastUsed.isAfter(Instant.now().minus(recentDays.days.toJavaDuration()))
        }
    }

    private fun AppFilterState.permissionPredicate(): ((InstalledApp) -> Boolean)? {
        if (selectedPermissions.isEmpty()) return null
        return if (permissionMatchAll) {
            { app -> selectedPermissions.all { it in app.requestedPermissions } }
        } else {
            { app -> app.requestedPermissions.any { it in selectedPermissions } }
        }
    }
}
