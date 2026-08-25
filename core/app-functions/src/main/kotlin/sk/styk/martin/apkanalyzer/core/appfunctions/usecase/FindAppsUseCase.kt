package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import androidx.appfunctions.AppFunctionInvalidArgumentException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.appfunctions.model.AppSummary
import sk.styk.martin.apkanalyzer.core.appfunctions.toAppFunctionLabel
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val MAX_RESULTS = 25

internal class FindAppsUseCase @Inject constructor(
    private val installedAppsRepository: InstalledAppsRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(
        query: String?,
        permission: String?,
        installSource: String?,
        targetSdk: Int?,
        minTotalSizeMb: Int?,
        unusedForDays: Int?,
        sortBy: String?,
    ): List<AppSummary> = withContext(dispatcherProvider.io()) {
        val normalizedQuery = query?.takeIf(String::isNotBlank)
        val normalizedPermission = permission?.takeIf(String::isNotBlank)?.toFullPermissionName()
        val normalizedSource = installSource?.takeIf(String::isNotBlank)?.toAppSource()
        validate(normalizedQuery, normalizedPermission, normalizedSource, targetSdk, minTotalSizeMb, unusedForDays)
        val comparator = (sortBy?.takeIf(String::isNotBlank) ?: "Name").toComparator()
        val now = Instant.now()

        installedAppsRepository.apps().first()
            .filter {
                it.matches(
                    query = normalizedQuery,
                    permission = normalizedPermission,
                    installSource = normalizedSource,
                    targetSdk = targetSdk,
                    minTotalSizeMb = minTotalSizeMb,
                    unusedForDays = unusedForDays,
                    now = now,
                )
            }
            .sortedWith(comparator)
            .take(MAX_RESULTS)
            .map { it.toAppSummary() }
    }
}

private fun validate(
    query: String?,
    permission: String?,
    installSource: AppSource?,
    targetSdk: Int?,
    minTotalSizeMb: Int?,
    unusedForDays: Int?,
) {
    requireAtLeastOneFilter(query, permission, installSource, targetSdk, minTotalSizeMb, unusedForDays)
    requirePositive(targetSdk, "targetSdk", "a positive Android API level")
    requirePositive(minTotalSizeMb, "minTotalSizeMb", "a positive number of megabytes")
    requirePositive(unusedForDays, "unusedForDays", "a positive number of days")
}

private fun requireAtLeastOneFilter(vararg filters: Any?) {
    if (filters.all { it == null }) {
        throw AppFunctionInvalidArgumentException(
            "Provide at least one of query, permission, installSource, targetSdk, minTotalSizeMb, or unusedForDays",
        )
    }
}

private fun requirePositive(
    value: Int?,
    name: String,
    expected: String,
) {
    if (value != null && value <= 0) {
        throw AppFunctionInvalidArgumentException("$name must be $expected")
    }
}

private fun InstalledApp.matches(
    query: String?,
    permission: String?,
    installSource: AppSource?,
    targetSdk: Int?,
    minTotalSizeMb: Int?,
    unusedForDays: Int?,
    now: Instant,
): Boolean {
    if (query != null && !applicationName.contains(query, ignoreCase = true) &&
        !packageName.value.contains(query, ignoreCase = true)
    ) {
        return false
    }
    if (permission != null && permission !in requestedPermissions) return false
    if (installSource != null && source != installSource) return false
    if (targetSdk != null && this.targetSdk != targetSdk) return false
    if (minTotalSizeMb != null && (totalSize ?: apkSize).megabytes < minTotalSizeMb) return false
    if (unusedForDays != null) {
        val cutoff = now.minus(unusedForDays.toLong(), ChronoUnit.DAYS)
        val usedRecently = lastUsedTime?.isAfter(cutoff) == true
        if (usedRecently) return false
    }
    return true
}

private fun String.toFullPermissionName(): String = if (contains('.')) this else "android.permission.${uppercase()}"

private fun String.toAppSource(): AppSource = runCatching { AppSource.valueOf(this) }
    .getOrElse { throw AppFunctionInvalidArgumentException("Unknown install source: $this") }

private fun String.toComparator(): Comparator<InstalledApp> = when (this) {
    "Name" -> compareBy { it.applicationName }

    "SizeDescending" -> compareByDescending { (it.totalSize ?: it.apkSize).bytes }

    "SizeAscending" -> compareBy { (it.totalSize ?: it.apkSize).bytes }

    "LastUsedAscending" -> compareBy(nullsFirst()) { it.lastUsedTime }

    "LastUsedDescending" -> Comparator { a, b ->
        val aLastUsed = a.lastUsedTime
        val bLastUsed = b.lastUsedTime
        when {
            aLastUsed == null && bLastUsed == null -> 0
            aLastUsed == null -> 1
            bLastUsed == null -> -1
            else -> bLastUsed.compareTo(aLastUsed)
        }
    }

    else -> throw AppFunctionInvalidArgumentException("Unknown sortBy: $this")
}

private fun InstalledApp.toAppSummary() = AppSummary(
    packageName = packageName.value,
    applicationName = applicationName,
    versionName = versionName,
    installSourceLabel = source.toAppFunctionLabel(),
    sizeMb = (totalSize ?: apkSize).megabytes,
    lastUsedTime = lastUsedTime,
)
