package sk.styk.martin.apkanalyzer.feature.appdetail.api

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.appdetail.api.serialization.EpochMillisInstantSerializer
import java.time.Instant

@Serializable
@Immutable
sealed interface AppDetailInput {

    @Serializable
    data class InstalledPackage(val packageName: String) : AppDetailInput

    @Serializable
    data class ApkFile(val apkFilePath: String, val lifetime: ApkFileLifetime = ApkFileLifetime.Persistent) : AppDetailInput

    @Serializable
    data class Historical(
        val packageName: String,
        val snapshotId: Long,
        @Serializable(with = EpochMillisInstantSerializer::class)
        val capturedAt: Instant,
    ) : AppDetailInput
}

@Serializable
enum class ApkFileLifetime {
    Persistent,
    Temporary,
}
