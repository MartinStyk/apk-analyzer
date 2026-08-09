package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import sk.styk.martin.apkanalyzer.core.apps.model.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

private val GOOGLE_PLAY_INSTALLER = PackageName("com.android.vending")

internal class InstallSourceResolverImpl @Inject constructor(private val packageManager: PackageManager) : InstallSourceResolver {

    @Suppress("DEPRECATION")
    override fun appInstallSourceChain(packageInfo: PackageInfo): InstallSourceChain {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return InstallSourceChain(
                installingPackage = runCatching { packageManager.getInstallerPackageName(packageInfo.packageName) }.getOrNull()?.let(::PackageName),
                initiatingPackage = null,
                originatingPackage = null,
            )
        }
        val installSourceInfo = runCatching { packageManager.getInstallSourceInfo(packageInfo.packageName) }.getOrNull()
        return InstallSourceChain(
            installingPackage = installSourceInfo?.installingPackageName?.let(::PackageName),
            initiatingPackage = installSourceInfo?.initiatingPackageName?.let(::PackageName),
            originatingPackage = installSourceInfo?.originatingPackageName?.let(::PackageName),
        )
    }

    override fun isSystemApp(packageInfo: PackageInfo): Boolean =
        packageInfo.applicationInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false

    override fun appSource(chain: InstallSourceChain, isSystemApp: Boolean): AppSource = when {
        chain.installingPackage == GOOGLE_PLAY_INSTALLER -> AppSource.GooglePlay
        isSystemApp -> AppSource.SystemPreinstalled
        else -> AppSource.Unknown
    }
}
