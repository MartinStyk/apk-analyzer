package sk.styk.martin.apkanalyzer.core.apps.model

data class Service(
    val name: String,
    val permission: String? = null,
    val isExported: Boolean = false,
    var isStopWithTask: Boolean = false,
    var isSingleUser: Boolean = false,
    var isIsolatedProcess: Boolean = false,
    var isExternalService: Boolean = false,
)
