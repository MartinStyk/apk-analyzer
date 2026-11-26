package sk.styk.martin.apkanalyzer.core.apps.installsource

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo

internal fun isSystemInstalledApp(packageInfo: PackageInfo): Boolean = packageInfo.applicationInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false
