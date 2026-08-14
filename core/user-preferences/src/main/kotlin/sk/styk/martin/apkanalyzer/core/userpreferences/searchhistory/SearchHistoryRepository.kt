package sk.styk.martin.apkanalyzer.core.userpreferences.searchhistory

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface SearchHistoryRepository {
    fun history(): Flow<List<SearchHistoryEntry>>
    suspend fun addSearch(packageName: PackageName, query: String)
    suspend fun removeEntry(packageName: PackageName)
    suspend fun clearAll()
}

data class SearchHistoryEntry(
    val packageName: PackageName,
    val query: String,
    val app: InstalledApp?,
)
