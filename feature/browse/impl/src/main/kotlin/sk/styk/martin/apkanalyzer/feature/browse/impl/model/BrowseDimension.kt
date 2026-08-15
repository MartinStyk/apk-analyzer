package sk.styk.martin.apkanalyzer.feature.browse.impl.model

import kotlinx.serialization.Serializable

@Serializable
internal enum class BrowseDimension {
    Permission,
    SigningCertificate,
    TargetSdk,
    MinSdk,
    InstallSource,
    SharedUserId,
    AppCategory,
}

internal val BrowseDimension.analyticsValue: String
    get() = when (this) {
        BrowseDimension.Permission -> "permission"
        BrowseDimension.SigningCertificate -> "signing_certificate"
        BrowseDimension.TargetSdk -> "target_sdk"
        BrowseDimension.MinSdk -> "min_sdk"
        BrowseDimension.InstallSource -> "install_source"
        BrowseDimension.SharedUserId -> "shared_user_id"
        BrowseDimension.AppCategory -> "app_category"
    }
