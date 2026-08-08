package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

private val GOOGLE_PLAY_INSTALLER = PackageName("com.android.vending")

internal class InstallSourceResolverImpl @Inject constructor(private val packageManager: PackageManager) : InstallSourceResolver {
    override fun getAppInstallSource(packageInfo: PackageInfo): AppSource = when {
        appInstallingPackage(packageInfo) == GOOGLE_PLAY_INSTALLER -> AppSource.GooglePlay
        isSystemInstalledApp(packageInfo) -> AppSource.SystemPreinstalled
        else -> AppSource.Unknown
    }

    override fun appInstallingPackage(packageInfo: PackageInfo): PackageName? = runCatching {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> packageManager.getInstallSourceInfo(packageInfo.packageName).installingPackageName
            else -> packageManager.getInstallerPackageName(packageInfo.packageName)
        }
    }.getOrNull()?.let(::PackageName)

    override fun isSystemInstalledApp(packageInfo: PackageInfo): Boolean =
        packageInfo.applicationInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false
}
