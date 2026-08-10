package sk.styk.martin.apkanalyzer.core.apps.installsource

import android.content.pm.PackageInfo

internal interface InstallSourceResolver {
    fun resolve(packageInfo: PackageInfo): InstallSourceChain
}
