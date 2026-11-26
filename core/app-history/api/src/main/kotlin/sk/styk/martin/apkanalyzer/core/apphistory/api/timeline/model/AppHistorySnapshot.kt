package sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model

import sk.styk.martin.apkanalyzer.core.apps.api.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeVersion
import java.time.Instant

sealed interface AppHistorySnapshotRead {
    data class Found(val snapshot: AppHistorySnapshot) : AppHistorySnapshotRead

    data object NotFound : AppHistorySnapshotRead

    data object Unavailable : AppHistorySnapshotRead
}

data class AppHistorySnapshot(
    val id: AppHistorySnapshotId,
    val capturedAt: Instant,
    val detail: AppDetail,
    val nativeLibraries: AppHistorySection<NativeLibraries>,
    val signingSchemes: AppHistorySection<List<SigningSchemeVersion>?>,
)

sealed interface AppHistorySection<out T> {
    data class Recorded<T>(val value: T) : AppHistorySection<T>

    data object NotRecorded : AppHistorySection<Nothing>
}
