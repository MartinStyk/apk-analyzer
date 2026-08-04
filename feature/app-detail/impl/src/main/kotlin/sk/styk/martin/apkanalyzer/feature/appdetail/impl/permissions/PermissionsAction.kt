package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

internal sealed interface PermissionsAction {
    data object Retry : PermissionsAction
    data object ClearNarrowing : PermissionsAction
    data class ChangeQuery(val query: String) : PermissionsAction
    data class SelectScope(val scope: PermissionScope) : PermissionsAction
    data class ToggleFilter(val filter: PermissionFilter) : PermissionsAction
    data class CopyValue(val label: String, val value: String) : PermissionsAction
}
