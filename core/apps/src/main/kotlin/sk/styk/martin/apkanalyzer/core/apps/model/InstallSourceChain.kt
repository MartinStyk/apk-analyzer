package sk.styk.martin.apkanalyzer.core.apps.model

import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

data class InstallSourceChain(
    val isSystemApp: Boolean = false,
    val source: AppSource = AppSource.Unknown,
    val installingPackage: PackageName? = null,
    val initiatingPackage: PackageName? = null,
    val originatingPackage: PackageName? = null,
)
