package sk.styk.martin.apkanalyzer.core.appaidescription.cache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppAiDescriptionEntity::class], version = 2, exportSchema = false)
internal abstract class AppAiDescriptionDatabase : RoomDatabase() {
    abstract fun appAiDescriptionDao(): AppAiDescriptionDao
}
