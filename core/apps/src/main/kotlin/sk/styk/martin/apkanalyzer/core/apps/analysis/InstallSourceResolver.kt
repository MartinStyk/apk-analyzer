package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.apps.model.InstallSourceChain

interface InstallSourceResolver {
    fun appInstallSourceChain(packageInfo: PackageInfo): InstallSourceChain
}
