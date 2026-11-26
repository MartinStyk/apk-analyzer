package sk.styk.martin.apkanalyzer.core.apps.installsource

import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.apps.R
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

@get:StringRes
val AppSource.labelRes: Int
    get() = when (this) {
        AppSource.GooglePlay -> R.string.general_info_install_source_google_play
        AppSource.SamsungGalaxyStore -> R.string.general_info_install_source_samsung_galaxy_store
        AppSource.AmazonAppstore -> R.string.general_info_install_source_amazon_appstore
        AppSource.HuaweiAppGallery -> R.string.general_info_install_source_huawei_app_gallery
        AppSource.XiaomiGetApps -> R.string.general_info_install_source_xiaomi_get_apps
        AppSource.FDroid -> R.string.general_info_install_source_fdroid
        AppSource.AuroraStore -> R.string.general_info_install_source_aurora_store
        AppSource.Sideloaded -> R.string.general_info_install_source_sideloaded
        AppSource.LocalInstall -> R.string.general_info_install_source_local_install
        AppSource.SystemPreinstalled -> R.string.general_info_install_source_system
        AppSource.Unknown -> R.string.general_info_install_source_unknown
    }

@get:StringRes
val AppSource.rationaleRes: Int
    get() = when (this) {
        AppSource.GooglePlay -> R.string.general_info_rationale_install_source_google_play
        AppSource.SamsungGalaxyStore -> R.string.general_info_rationale_install_source_samsung_galaxy_store
        AppSource.AmazonAppstore -> R.string.general_info_rationale_install_source_amazon_appstore
        AppSource.HuaweiAppGallery -> R.string.general_info_rationale_install_source_huawei_app_gallery
        AppSource.XiaomiGetApps -> R.string.general_info_rationale_install_source_xiaomi_get_apps
        AppSource.FDroid -> R.string.general_info_rationale_install_source_fdroid
        AppSource.AuroraStore -> R.string.general_info_rationale_install_source_aurora_store
        AppSource.Sideloaded -> R.string.general_info_rationale_install_source_sideloaded
        AppSource.LocalInstall -> R.string.general_info_rationale_install_source_local_install
        AppSource.SystemPreinstalled -> R.string.general_info_rationale_install_source_system
        AppSource.Unknown -> R.string.general_info_rationale_install_source_unknown
    }
