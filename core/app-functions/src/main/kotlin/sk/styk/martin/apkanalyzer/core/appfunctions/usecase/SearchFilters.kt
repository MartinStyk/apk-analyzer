package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import androidx.appfunctions.AppFunctionInvalidArgumentException
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

internal data class SearchFilters(
    val query: String?,
    val permission: String?,
    val installSource: AppSource?,
    val targetSdk: Int?,
    val minTotalSizeMb: Int?,
    val unusedForDays: Int?,
) {
    val isEmpty: Boolean
        get() = query == null && permission == null && installSource == null &&
            targetSdk == null && minTotalSizeMb == null && unusedForDays == null

    companion object {
        fun parse(
            query: String?,
            permission: String?,
            installSource: String?,
            targetSdk: Int?,
            minTotalSizeMb: Int?,
            unusedForDays: Int?,
        ): SearchFilters {
            requirePositive(targetSdk, "targetSdk", "a positive Android API level")
            requirePositive(minTotalSizeMb, "minTotalSizeMb", "a positive number of megabytes")
            requirePositive(unusedForDays, "unusedForDays", "a positive number of days")
            return SearchFilters(
                query = query?.takeIf(String::isNotBlank),
                permission = permission?.takeIf(String::isNotBlank)?.toFullPermissionName(),
                installSource = installSource?.takeIf(String::isNotBlank)?.toAppSource(),
                targetSdk = targetSdk,
                minTotalSizeMb = minTotalSizeMb,
                unusedForDays = unusedForDays,
            )
        }
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

private fun String.toFullPermissionName(): String = if (contains('.')) this else "android.permission.${uppercase()}"

private fun String.toAppSource(): AppSource = runCatching { AppSource.valueOf(this) }
    .getOrElse { throw AppFunctionInvalidArgumentException("Unknown install source: $this") }
