package sk.styk.martin.apkanalyzer.core.apps.components

data class BroadcastReceiver(
    val name: String,
    val permission: String? = null,
    val isExported: Boolean = false,
    val intentFilters: List<ComponentIntentFilter> = emptyList(),
)

val BroadcastReceiver.isExternallyReachableWithoutPermission: Boolean
    get() = isExported && permission.isNullOrBlank()
