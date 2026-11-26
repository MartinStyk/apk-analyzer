package sk.styk.martin.apkanalyzer.feature.appdetail.tv.permissions

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.api.detail.PermissionScope
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.ProtectionLevel

@Immutable
internal data class TvPermissionScopeOption(val scope: PermissionScope, val count: Int)

@Immutable
internal data class TvPermissionSection(val protectionLevel: ProtectionLevel?, val permissions: List<PermissionItem>)

@Immutable
internal sealed interface TvPermissionsState {
    data object Loading : TvPermissionsState

    data object Error : TvPermissionsState

    @Immutable
    data class Loaded(
        val scope: PermissionScope,
        val scopeOptions: List<TvPermissionScopeOption>,
        val sections: List<TvPermissionSection>,
        val initialPermissionName: String?,
    ) : TvPermissionsState {
        val hasScopeChoice: Boolean
            get() = scopeOptions.size > 1
    }
}
