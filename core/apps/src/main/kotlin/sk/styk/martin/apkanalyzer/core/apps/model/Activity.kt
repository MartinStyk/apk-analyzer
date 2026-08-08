package sk.styk.martin.apkanalyzer.core.apps.model

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

data class Activity(
    val name: String,
    val packageName: PackageName,
    val label: String? = null,
    val targetActivity: String? = null,
    val permission: String? = null,
    val parentName: String? = null,
    val isExported: Boolean = false,
    val isLauncher: Boolean? = null,
    val intentFilters: List<ComponentIntentFilter> = emptyList(),
)

val Activity.isExternallyReachableWithoutPermission: Boolean
    get() = isExported && isLauncher != true && permission.isNullOrBlank()
