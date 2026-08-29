package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import androidx.appfunctions.AppFunctionInvalidArgumentException

internal enum class SortBy {
    Name,
    SizeDescending,
    SizeAscending,
    LastUsedAscending,
    LastUsedDescending,
    ;

    companion object {
        fun parse(value: String?): SortBy? {
            val normalized = value?.takeIf(String::isNotBlank) ?: return null
            return entries.find { it.name == normalized }
                ?: throw AppFunctionInvalidArgumentException("Unknown sortBy: $normalized")
        }
    }
}
