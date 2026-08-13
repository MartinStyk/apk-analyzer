package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_ai_description")
internal data class AppAiDescriptionEntity(
    @PrimaryKey val packageName: String,
    val inputHash: String,
    val description: String,
    val generatedAt: Long,
)
