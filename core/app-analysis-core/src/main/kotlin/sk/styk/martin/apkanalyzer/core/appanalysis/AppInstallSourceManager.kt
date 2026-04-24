package sk.styk.martin.apkanalyzer.core.appanalysis

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import javax.inject.Inject

class AppInstallSourceManager
@Inject
constructor(private val packageManager: PackageManager) {
    fun getAppInstallSource(packageInfo: PackageInfo): AppSource {
        val installer = appInstallingPackage(packageInfo)

        return when (installer) {
            GOOGLE_PLAY_INSTALLER -> AppSource.GooglePlay
            AMAZON_STORE_INSTALLER -> AppSource.AmazonStore
            else -> if (isSystemInstalledApp(packageInfo)) AppSource.SystemPreinstalled else AppSource.Unknown
        }
    }

    fun appInstallingPackage(packageInfo: PackageInfo): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageInfo.packageName).installingPackageName
        } else {
            packageManager.getInstallerPackageName(packageInfo.packageName)
        }
    } catch (_: Exception) {
        null
    }

    fun isSystemInstalledApp(packageInfo: PackageInfo): Boolean = packageInfo.applicationInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false

    private companion object {
        const val GOOGLE_PLAY_INSTALLER = "com.android.vending"
        const val AMAZON_STORE_INSTALLER = "com.amazon.venezia"
    }
}
