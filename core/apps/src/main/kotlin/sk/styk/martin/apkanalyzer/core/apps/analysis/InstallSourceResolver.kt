package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.apps.model.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

interface InstallSourceResolver {
    fun appInstallSourceChain(packageInfo: PackageInfo): InstallSourceChain
    fun isSystemApp(packageInfo: PackageInfo): Boolean
    fun appSource(chain: InstallSourceChain, isSystemApp: Boolean): AppSource
}
