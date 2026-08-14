package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.cache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppAiDescriptionEntity::class], version = 1, exportSchema = false)
internal abstract class AppAiDescriptionDatabase : RoomDatabase() {
    abstract fun appAiDescriptionDao(): AppAiDescriptionDao
}
