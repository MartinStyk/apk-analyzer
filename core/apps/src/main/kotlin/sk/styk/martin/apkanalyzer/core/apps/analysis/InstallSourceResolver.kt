package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface InstallSourceResolver {
    fun getAppInstallSource(packageInfo: PackageInfo): AppSource
    fun appInstallingPackage(packageInfo: PackageInfo): PackageName?
    fun isSystemInstalledApp(packageInfo: PackageInfo): Boolean
}
