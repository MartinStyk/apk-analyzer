package sk.styk.martin.apkanalyzer.core.appanalysis

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import sk.styk.martin.apkanalyzer.core.appanalysis.model.AppSource
import javax.inject.Inject

class AppInstallSourceManager
@Inject
constructor(private val packageManager: PackageManager) {
    fun getAppInstallSource(packageInfo: PackageInfo): AppSource {
        val installer = appInstallingPackage(packageInfo)

        return if (installer == AppSource.GOOGLE_PLAY.installerPackageName) {
            AppSource.GOOGLE_PLAY
        } else if (installer == AppSource.AMAZON_STORE.installerPackageName) {
            AppSource.AMAZON_STORE
        } else if (installer == AppSource.SYSTEM_PREINSTALED.installerPackageName || isSystemInstalledApp(packageInfo)) {
            AppSource.SYSTEM_PREINSTALED
        } else {
            AppSource.UNKNOWN
        }
    }

    fun appInstallingPackage(packageInfo: PackageInfo): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageInfo.packageName).installingPackageName
        } else {
            packageManager.getInstallerPackageName(packageInfo.packageName)
        }
    } catch (e: Exception) {
        null // this means package is not installed
    }

    fun isSystemInstalledApp(packageInfo: PackageInfo): Boolean = packageInfo.applicationInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false
}
