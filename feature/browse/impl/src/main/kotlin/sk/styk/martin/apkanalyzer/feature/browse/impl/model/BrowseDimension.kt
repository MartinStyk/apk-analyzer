package sk.styk.martin.apkanalyzer.feature.browse.impl.model

import kotlinx.serialization.Serializable

@Serializable
internal enum class BrowseDimension {
    Permission,
    SigningCertificate,
    TargetSdk,
    MinSdk,
    InstallSource,
}
