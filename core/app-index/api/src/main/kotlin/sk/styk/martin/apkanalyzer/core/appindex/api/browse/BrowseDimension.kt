package sk.styk.martin.apkanalyzer.core.appindex.api.browse

import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.appindex.api.R

enum class BrowseDimension {
    Permission,
    SigningCertificate,
    TargetSdk,
    MinSdk,
    InstallSource,
    SharedUserId,
    AppCategory,
}

@get:StringRes
val BrowseDimension.titleRes: Int
    get() = when (this) {
        BrowseDimension.Permission -> R.string.browse_dimension_permission_title
        BrowseDimension.SigningCertificate -> R.string.browse_dimension_certificate_title
        BrowseDimension.TargetSdk -> R.string.browse_dimension_target_sdk_title
        BrowseDimension.MinSdk -> R.string.browse_dimension_min_sdk_title
        BrowseDimension.InstallSource -> R.string.browse_dimension_install_source_title
        BrowseDimension.SharedUserId -> R.string.browse_dimension_shared_user_id_title
        BrowseDimension.AppCategory -> R.string.browse_dimension_app_category_title
    }

@get:StringRes
val BrowseDimension.subtitleRes: Int
    get() = when (this) {
        BrowseDimension.Permission -> R.string.browse_dimension_permission_subtitle
        BrowseDimension.SigningCertificate -> R.string.browse_dimension_certificate_subtitle
        BrowseDimension.TargetSdk -> R.string.browse_dimension_target_sdk_subtitle
        BrowseDimension.MinSdk -> R.string.browse_dimension_min_sdk_subtitle
        BrowseDimension.InstallSource -> R.string.browse_dimension_install_source_subtitle
        BrowseDimension.SharedUserId -> R.string.browse_dimension_shared_user_id_subtitle
        BrowseDimension.AppCategory -> R.string.browse_dimension_app_category_subtitle
    }

val BrowseDimension.isSearchable: Boolean
    get() = this != BrowseDimension.InstallSource
