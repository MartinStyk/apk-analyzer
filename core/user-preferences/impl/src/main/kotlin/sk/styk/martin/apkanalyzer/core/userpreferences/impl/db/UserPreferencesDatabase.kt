package sk.styk.martin.apkanalyzer.core.userpreferences.impl.db

import androidx.room.Database
import androidx.room.RoomDatabase
import sk.styk.martin.apkanalyzer.core.userpreferences.impl.recentlyviewed.RecentlyViewedAppDao
import sk.styk.martin.apkanalyzer.core.userpreferences.impl.recentlyviewed.RecentlyViewedAppEntity
import sk.styk.martin.apkanalyzer.core.userpreferences.impl.searchhistory.SearchHistoryDao
import sk.styk.martin.apkanalyzer.core.userpreferences.impl.searchhistory.SearchHistoryEntity

@Database(
    entities = [RecentlyViewedAppEntity::class, SearchHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class UserPreferencesDatabase : RoomDatabase() {
    abstract fun recentlyViewedAppDao(): RecentlyViewedAppDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
