package sk.styk.martin.apkanalyzer.core.apppermissions.detail

import sk.styk.martin.apkanalyzer.core.apps.api.permissions.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

enum class PermissionScope {
    Requested,
    Defined,
}

enum class GrantState {
    Granted,
    NotGranted,
}

data class PermissionItem(
    val name: String,
    val label: String,
    val description: String?,
    val groupName: String?,
    val protectionLevel: ProtectionLevel?,
    val protectionFlags: List<ProtectionFlag>,
    val grantState: GrantState?,
    val declaringPackage: PackageName?,
    val isSelfDeclared: Boolean,
) {
    val displayGroupName: String?
        get() = groupName?.takeIf { it != UNDEFINED_PERMISSION_GROUP }
}

private const val UNDEFINED_PERMISSION_GROUP = "android.permission-group.UNDEFINED"
