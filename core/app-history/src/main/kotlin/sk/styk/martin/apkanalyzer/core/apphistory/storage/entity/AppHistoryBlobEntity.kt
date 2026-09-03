package sk.styk.martin.apkanalyzer.core.apphistory.storage.entity

import androidx.room.Entity

@Entity(
    tableName = "app_history_blob",
    primaryKeys = ["packageName", "hash"],
)
internal data class AppHistoryBlobEntity(
    val packageName: String,
    val hash: String,
    val sectionType: SectionType,
    val content: String,
)

internal enum class SectionType {
    Permissions,
    Activities,
    Services,
    Receivers,
    Providers,
    Features,
    Signing,
    IntentFilters,
    NativeLibraries,
    SigningScheme,
    InstalledSplits,
}
