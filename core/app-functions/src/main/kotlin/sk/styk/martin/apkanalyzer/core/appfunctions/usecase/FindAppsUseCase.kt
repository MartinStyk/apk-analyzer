package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import androidx.appfunctions.AppFunctionInvalidArgumentException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.appfunctions.model.AppSummary
import sk.styk.martin.apkanalyzer.core.appfunctions.toAppFunctionLabel
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val MAX_RESULTS = 25

internal class FindAppsUseCase @Inject constructor(
    private val installedAppsRepository: InstalledAppsRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(filters: SearchFilters, sortBy: SortBy?): List<AppSummary> = withContext(dispatcherProvider.io()) {
        if (filters.isEmpty && sortBy == null) {
            throw AppFunctionInvalidArgumentException(
                "Provide at least one of query, permission, installSource, targetSdk, minTotalSizeMb, unusedForDays, or sortBy",
            )
        }
        val comparator = (sortBy ?: SortBy.Name).toComparator()
        val now = Instant.now()

        installedAppsRepository.apps().first()
            .filter { it.matches(filters, now) }
            .sortedWith(comparator)
            .take(MAX_RESULTS)
            .map { it.toAppSummary() }
    }
}

private fun InstalledApp.matches(filters: SearchFilters, now: Instant): Boolean {
    val query = filters.query
    if (query != null && !applicationName.contains(query, ignoreCase = true) &&
        !packageName.value.contains(query, ignoreCase = true)
    ) {
        return false
    }
    if (filters.permission != null && filters.permission !in requestedPermissions) return false
    if (filters.installSource != null && source != filters.installSource) return false
    if (filters.targetSdk != null && targetSdk != filters.targetSdk) return false
    if (filters.minTotalSizeMb != null && (totalSize ?: apkSize).megabytes < filters.minTotalSizeMb) return false
    if (filters.unusedForDays != null) {
        val cutoff = now.minus(filters.unusedForDays.toLong(), ChronoUnit.DAYS)
        val usedRecently = lastUsedTime?.isAfter(cutoff) == true
        if (usedRecently) return false
    }
    return true
}

private fun SortBy.toComparator(): Comparator<InstalledApp> = when (this) {
    SortBy.Name -> compareBy { it.applicationName }

    SortBy.SizeDescending -> compareByDescending { (it.totalSize ?: it.apkSize).bytes }

    SortBy.SizeAscending -> compareBy { (it.totalSize ?: it.apkSize).bytes }

    SortBy.LastUsedAscending -> compareBy(nullsFirst()) { it.lastUsedTime }

    SortBy.LastUsedDescending -> Comparator { a, b ->
        val aLastUsed = a.lastUsedTime
        val bLastUsed = b.lastUsedTime
        when {
            aLastUsed == null && bLastUsed == null -> 0
            aLastUsed == null -> 1
            bLastUsed == null -> -1
            else -> bLastUsed.compareTo(aLastUsed)
        }
    }
}

private fun InstalledApp.toAppSummary() = AppSummary(
    packageName = packageName.value,
    applicationName = applicationName,
    versionName = versionName,
    installSourceLabel = source.toAppFunctionLabel(),
    sizeMb = (totalSize ?: apkSize).megabytes,
    lastUsedTime = lastUsedTime,
)
