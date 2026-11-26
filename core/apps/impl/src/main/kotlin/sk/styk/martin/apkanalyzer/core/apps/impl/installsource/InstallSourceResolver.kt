package sk.styk.martin.apkanalyzer.core.apps.impl.installsource

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.apps.api.installsource.InstallSourceChain

internal interface InstallSourceResolver {
    fun resolve(packageInfo: PackageInfo): InstallSourceChain
}
