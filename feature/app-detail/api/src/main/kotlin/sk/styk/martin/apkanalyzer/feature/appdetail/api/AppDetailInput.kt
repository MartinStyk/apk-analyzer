package sk.styk.martin.apkanalyzer.feature.appdetail.api

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
sealed interface AppDetailInput {

    @Serializable
    data class InstalledPackage(val packageName: String) : AppDetailInput

    @Serializable
    data class ApkFile(val apkFilePath: String, val lifetime: ApkFileLifetime = ApkFileLifetime.Persistent) : AppDetailInput
}

@Serializable
enum class ApkFileLifetime {
    Persistent,
    Temporary,
}
