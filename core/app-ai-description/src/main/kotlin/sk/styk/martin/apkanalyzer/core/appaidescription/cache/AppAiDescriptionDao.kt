package sk.styk.martin.apkanalyzer.core.appaidescription.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface AppAiDescriptionDao {
    @Query("SELECT * FROM app_ai_description WHERE packageName = :packageName AND inputHash = :inputHash")
    suspend fun get(packageName: String, inputHash: String): AppAiDescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppAiDescriptionEntity)
}
