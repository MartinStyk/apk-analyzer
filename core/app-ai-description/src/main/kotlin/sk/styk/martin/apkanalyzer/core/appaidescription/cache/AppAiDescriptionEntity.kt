package sk.styk.martin.apkanalyzer.core.appaidescription.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_ai_description")
internal data class AppAiDescriptionEntity(
    @PrimaryKey val packageName: String,
    val versionCode: Long,
    val inputHash: String,
    val shortDescription: String,
    val longDescription: String,
    val generatedAt: Long,
)
