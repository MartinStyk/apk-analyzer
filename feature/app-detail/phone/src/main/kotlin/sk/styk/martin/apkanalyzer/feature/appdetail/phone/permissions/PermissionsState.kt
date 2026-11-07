package sk.styk.martin.apkanalyzer.feature.appdetail.phone.permissions

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.GrantState
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionScope
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.RecordedVersionFailure

@Immutable
internal data class PermissionSection(val protectionLevel: ProtectionLevel?, val permissions: List<PermissionItem>)

@Immutable
internal sealed interface PermissionsState {
    data object Loading : PermissionsState

    data object Error : PermissionsState

    @Immutable
    data class RecordedVersionError(val failure: RecordedVersionFailure) : PermissionsState

    @Immutable
    data class Loaded(
        val scope: PermissionScope,
        val scopeOptions: List<PermissionScope>,
        val selectedProtectionLevels: Set<ProtectionLevel?>,
        val protectionLevelOptions: List<ProtectionLevel?>,
        val selectedGrantStates: Set<GrantState>,
        val grantStateOptions: List<GrantState>,
        val query: String,
        val scopeTotal: Int,
        val sections: List<PermissionSection>,
        val isHistorical: Boolean = false,
    ) : PermissionsState {
        val hasScopeChoice: Boolean
            get() = scopeOptions.size > 1

        val hasResults: Boolean
            get() = sections.isNotEmpty()

        val isNarrowed: Boolean
            get() = query.isNotBlank() || selectedProtectionLevels.isNotEmpty() || selectedGrantStates.isNotEmpty()
    }
}
