package sk.styk.martin.apkanalyzer.core.apps.model

data class Permissions(
    val defined: List<Permission>,
    val used: List<UsedPermission>,
)
