package sk.styk.martin.apkanalyzer.core.userpreferences.db

import androidx.room.Database
import androidx.room.RoomDatabase
import sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed.RecentlyViewedAppDao
import sk.styk.martin.apkanalyzer.core.userpreferences.recentlyviewed.RecentlyViewedAppEntity
import sk.styk.martin.apkanalyzer.core.userpreferences.searchhistory.SearchHistoryDao
import sk.styk.martin.apkanalyzer.core.userpreferences.searchhistory.SearchHistoryEntity

@Database(
    entities = [RecentlyViewedAppEntity::class, SearchHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class UserPreferencesDatabase : RoomDatabase() {
    abstract fun recentlyViewedAppDao(): RecentlyViewedAppDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
