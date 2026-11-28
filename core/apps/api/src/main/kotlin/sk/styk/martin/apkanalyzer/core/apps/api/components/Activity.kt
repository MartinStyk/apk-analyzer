package sk.styk.martin.apkanalyzer.core.apps.api.components

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
)

fun Activity.isExternallyReachableWithoutPermission(isDisabled: Boolean): Boolean =
    !isDisabled && isExported && isLauncher != true && permission.isNullOrBlank()

fun Activity.isLaunchable(isDisabled: Boolean): Boolean = !isDisabled && isExported && permission.isNullOrBlank()
