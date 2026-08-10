package sk.styk.martin.apkanalyzer.core.apps.installsource

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

private val STORE_INSTALLERS = mapOf(
    PackageName("com.android.vending") to AppSource.GooglePlay,
    PackageName("com.sec.android.app.samsungapps") to AppSource.SamsungGalaxyStore,
    PackageName("com.amazon.venezia") to AppSource.AmazonAppstore,
    PackageName("com.huawei.appmarket") to AppSource.HuaweiAppGallery,
    PackageName("com.xiaomi.market") to AppSource.XiaomiGetApps,
    PackageName("com.xiaomi.mipicks") to AppSource.XiaomiGetApps,
    PackageName("org.fdroid.fdroid") to AppSource.FDroid,
    PackageName("com.aurora.store") to AppSource.AuroraStore,
)

private val SIDELOAD_INSTALLERS = setOf(
    PackageName("com.google.android.packageinstaller"),
    PackageName("com.android.packageinstaller"),
    PackageName("com.android.chrome"),
    PackageName("org.mozilla.firefox"),
    PackageName("com.sec.android.app.sbrowser"),
    PackageName("com.microsoft.emmx"),
    PackageName("com.opera.browser"),
    PackageName("com.brave.browser"),
)

private val LOCAL_INSTALL_INSTALLERS = setOf(
    PackageName("pc"),
    PackageName("com.android.shell"),
)

fun isSystemInstalledApp(packageInfo: PackageInfo): Boolean = packageInfo.applicationInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false

fun resolveAppInstallSource(installSourceChain: InstallSourceChain, isSystemApp: Boolean): AppSource {
    val storeSource = installSourceChain.installingPackage?.let(STORE_INSTALLERS::get)
    return when {
        storeSource != null -> storeSource
        isSystemApp -> AppSource.SystemPreinstalled
        installSourceChain.matchesEither(LOCAL_INSTALL_INSTALLERS) -> AppSource.LocalInstall
        installSourceChain.matchesEither(SIDELOAD_INSTALLERS) -> AppSource.Sideloaded
        else -> AppSource.Unknown
    }
}

private fun InstallSourceChain.matchesEither(installers: Set<PackageName>): Boolean {
    val installing = installingPackage
    val initiating = initiatingPackage
    return (installing != null && installing in installers) || (initiating != null && initiating in installers)
}
