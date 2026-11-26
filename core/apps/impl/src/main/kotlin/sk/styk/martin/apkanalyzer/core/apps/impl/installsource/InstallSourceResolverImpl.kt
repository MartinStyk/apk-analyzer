package sk.styk.martin.apkanalyzer.core.apps.installsource

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class InstallSourceResolverImpl @Inject constructor(private val packageManager: PackageManager) : InstallSourceResolver {

    @Suppress("DEPRECATION")
    override fun resolve(packageInfo: PackageInfo): InstallSourceChain {
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
}
