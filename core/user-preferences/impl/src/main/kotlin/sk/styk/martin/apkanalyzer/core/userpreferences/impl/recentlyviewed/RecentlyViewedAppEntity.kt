package sk.styk.martin.apkanalyzer.core.userpreferences.impl.recentlyviewed

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recently_viewed_apps")
internal data class RecentlyViewedAppEntity(
    @PrimaryKey val packageName: String,
    val viewCount: Int,
    val lastViewedAt: Long,
)
