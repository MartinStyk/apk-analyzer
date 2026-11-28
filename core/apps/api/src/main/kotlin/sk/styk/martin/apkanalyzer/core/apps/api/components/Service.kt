package sk.styk.martin.apkanalyzer.core.apps.api.components

data class Service(
    val name: String,
    val permission: String? = null,
    val isExported: Boolean = false,
    val isStopWithTask: Boolean = false,
    val isSingleUser: Boolean = false,
    val isIsolatedProcess: Boolean = false,
    val isExternalService: Boolean = false,
)

fun Service.isExternallyReachableWithoutPermission(isDisabled: Boolean): Boolean = !isDisabled && isExported && permission.isNullOrBlank()
