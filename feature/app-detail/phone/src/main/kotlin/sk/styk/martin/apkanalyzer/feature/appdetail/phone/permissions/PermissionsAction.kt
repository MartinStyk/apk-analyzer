package sk.styk.martin.apkanalyzer.feature.appdetail.phone.permissions

import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.GrantState
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionScope
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.ProtectionLevel

internal sealed interface PermissionsAction {
    data object Retry : PermissionsAction
    data object ClearNarrowing : PermissionsAction
    data class ChangeQuery(val query: String) : PermissionsAction
    data class SelectScope(val scope: PermissionScope) : PermissionsAction
    data class ToggleProtectionLevel(val protectionLevel: ProtectionLevel?) : PermissionsAction
    data class ToggleGrantState(val grantState: GrantState) : PermissionsAction
    data class CopyValue(val label: String, val value: String) : PermissionsAction
}
