package sk.styk.martin.apkanalyzer.core.apppermissions.api

interface PermissionLabelProvider {
    fun getLabel(permissionName: String): String
}
