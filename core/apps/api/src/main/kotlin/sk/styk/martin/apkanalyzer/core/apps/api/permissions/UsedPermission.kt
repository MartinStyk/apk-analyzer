package sk.styk.martin.apkanalyzer.core.apps.permissions

data class UsedPermission(val permissionData: Permission, val isGranted: Boolean) {
    val isDangerous: Boolean
        get() = permissionData.details?.protectionLevel == ProtectionLevel.Dangerous
}
