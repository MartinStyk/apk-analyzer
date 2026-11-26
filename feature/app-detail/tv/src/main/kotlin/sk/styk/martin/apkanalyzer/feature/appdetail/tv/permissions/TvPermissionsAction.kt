package sk.styk.martin.apkanalyzer.feature.appdetail.tv.permissions

import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionScope

internal sealed interface TvPermissionsAction {
    data object Retry : TvPermissionsAction
    data class SelectScope(val scope: PermissionScope) : TvPermissionsAction
}
