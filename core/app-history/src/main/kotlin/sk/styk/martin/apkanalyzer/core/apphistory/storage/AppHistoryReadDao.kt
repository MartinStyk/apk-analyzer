package sk.styk.martin.apkanalyzer.core.apphistory.storage

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import sk.styk.martin.apkanalyzer.core.apphistory.storage.entity.AppHistorySnapshotEntity

internal data class AppHistorySnapshot(
    @Embedded val snapshot: AppHistorySnapshotEntity,
    val permissionsContent: String?,
    val activitiesContent: String?,
    val servicesContent: String?,
    val receiversContent: String?,
    val providersContent: String?,
    val featuresContent: String?,
    val signingContent: String?,
    val intentFiltersContent: String?,
    val nativeLibrariesContent: String?,
    val signingSchemeContent: String?,
    val installedSplitsContent: String?,
)

@Dao
internal interface AppHistoryReadDao {

    @Query(
        """
        SELECT s.*,
            pb.content AS permissionsContent,
            ab.content AS activitiesContent,
            svb.content AS servicesContent,
            rb.content AS receiversContent,
            prb.content AS providersContent,
            fb.content AS featuresContent,
            sib.content AS signingContent,
            ifb.content AS intentFiltersContent,
            nlb.content AS nativeLibrariesContent,
            ssb.content AS signingSchemeContent,
            isb.content AS installedSplitsContent
        FROM app_history_snapshot s
        LEFT JOIN app_history_blob pb ON pb.packageName = s.packageName AND pb.hash = s.permissionsHash
        LEFT JOIN app_history_blob ab ON ab.packageName = s.packageName AND ab.hash = s.activitiesHash
        LEFT JOIN app_history_blob svb ON svb.packageName = s.packageName AND svb.hash = s.servicesHash
        LEFT JOIN app_history_blob rb ON rb.packageName = s.packageName AND rb.hash = s.receiversHash
        LEFT JOIN app_history_blob prb ON prb.packageName = s.packageName AND prb.hash = s.providersHash
        LEFT JOIN app_history_blob fb ON fb.packageName = s.packageName AND fb.hash = s.featuresHash
        LEFT JOIN app_history_blob sib ON sib.packageName = s.packageName AND sib.hash = s.signingHash
        LEFT JOIN app_history_blob ifb ON ifb.packageName = s.packageName AND ifb.hash = s.intentFiltersHash
        LEFT JOIN app_history_blob nlb ON nlb.packageName = s.packageName AND nlb.hash = s.nativeLibrariesHash
        LEFT JOIN app_history_blob ssb ON ssb.packageName = s.packageName AND ssb.hash = s.signingSchemeHash
        LEFT JOIN app_history_blob isb ON isb.packageName = s.packageName AND isb.hash = s.installedSplitsHash
        WHERE s.id = :id
        """,
    )
    suspend fun snapshotWithSections(id: Long): AppHistorySnapshot?
}
