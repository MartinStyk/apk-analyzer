package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal enum class PermissionScope {
    Requested,
    Defined,
}

internal enum class GrantState {
    Granted,
    NotGranted,
}

@Immutable
internal data class PermissionItem(
    val name: String,
    val label: String,
    val description: String?,
    val groupName: String?,
    val protectionLevel: ProtectionLevel?,
    val protectionFlags: List<ProtectionFlag>,
    val grantState: GrantState?,
    val declaringPackage: PackageName?,
    val isSelfDeclared: Boolean,
)

@Immutable
internal data class PermissionSection(val protectionLevel: ProtectionLevel?, val permissions: List<PermissionItem>)

@Immutable
internal sealed interface PermissionsState {
    data object Loading : PermissionsState

    data object Error : PermissionsState

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
    ) : PermissionsState {
        val hasScopeChoice: Boolean
            get() = scopeOptions.size > 1

        val hasResults: Boolean
            get() = sections.isNotEmpty()

        val isNarrowed: Boolean
            get() = query.isNotBlank() || selectedProtectionLevels.isNotEmpty() || selectedGrantStates.isNotEmpty()
    }
}
