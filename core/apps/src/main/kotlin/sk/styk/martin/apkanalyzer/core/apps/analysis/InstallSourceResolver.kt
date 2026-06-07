package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

interface InstallSourceResolver {
    fun getAppInstallSource(packageInfo: PackageInfo): AppSource
    fun appInstallingPackage(packageInfo: PackageInfo): String?
    fun isSystemInstalledApp(packageInfo: PackageInfo): Boolean
}
