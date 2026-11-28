package sk.styk.martin.apkanalyzer.core.apps.api.components

data class BroadcastReceiver(
    val name: String,
    val permission: String? = null,
    val isExported: Boolean = false,
)

fun BroadcastReceiver.isExternallyReachableWithoutPermission(isDisabled: Boolean): Boolean = !isDisabled && isExported && permission.isNullOrBlank()

fun BroadcastReceiver.isLaunchable(isDisabled: Boolean): Boolean = !isDisabled && isExported && permission.isNullOrBlank()
