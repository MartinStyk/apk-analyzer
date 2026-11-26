package sk.styk.martin.apkanalyzer.core.apps.installsource

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

data class InstallSourceChain(
    val installingPackage: PackageName? = null,
    val initiatingPackage: PackageName? = null,
    val originatingPackage: PackageName? = null,
)
