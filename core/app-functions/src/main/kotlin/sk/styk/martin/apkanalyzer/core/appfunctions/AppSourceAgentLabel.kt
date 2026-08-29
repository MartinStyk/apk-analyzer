package sk.styk.martin.apkanalyzer.core.appfunctions

import sk.styk.martin.apkanalyzer.core.common.model.AppSource

internal fun AppSource.toAppFunctionLabel(): String = when (this) {
    AppSource.GooglePlay -> "Google Play"
    AppSource.SamsungGalaxyStore -> "Samsung Galaxy Store"
    AppSource.AmazonAppstore -> "Amazon Appstore"
    AppSource.HuaweiAppGallery -> "Huawei AppGallery"
    AppSource.XiaomiGetApps -> "Xiaomi GetApps"
    AppSource.FDroid -> "F-Droid"
    AppSource.AuroraStore -> "Aurora Store"
    AppSource.Sideloaded -> "Sideloaded"
    AppSource.LocalInstall -> "Installed locally (ADB or file manager)"
    AppSource.SystemPreinstalled -> "Preinstalled system app"
    AppSource.Unknown -> "Unknown source"
}
