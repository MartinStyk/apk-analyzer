package sk.styk.martin.apkanalyzer.core.userpreferences.searchhistory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
internal data class SearchHistoryEntity(
    @PrimaryKey val packageName: String,
    val query: String,
    val searchCount: Int,
    val lastSearchedAt: Long,
)
