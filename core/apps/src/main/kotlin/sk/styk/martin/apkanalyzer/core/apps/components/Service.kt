package sk.styk.martin.apkanalyzer.core.apps.components

data class Service(
    val name: String,
    val permission: String? = null,
    val isExported: Boolean = false,
    val isStopWithTask: Boolean = false,
    val isSingleUser: Boolean = false,
    val isIsolatedProcess: Boolean = false,
    val isExternalService: Boolean = false,
)

val Service.isExternallyReachableWithoutPermission: Boolean
    get() = isExported && permission.isNullOrBlank()
