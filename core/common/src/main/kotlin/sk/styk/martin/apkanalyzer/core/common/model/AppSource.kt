package sk.styk.martin.apkanalyzer.core.common.model

enum class AppSource {
    GooglePlay,
    SamsungGalaxyStore,
    AmazonAppstore,
    HuaweiAppGallery,
    XiaomiGetApps,
    FDroid,
    AuroraStore,
    Sideloaded,
    LocalInstall,
    SystemPreinstalled,
    Unknown,
}

val AppSource.isSideloaded: Boolean
    get() = this == AppSource.Sideloaded || this == AppSource.LocalInstall || this == AppSource.Unknown
