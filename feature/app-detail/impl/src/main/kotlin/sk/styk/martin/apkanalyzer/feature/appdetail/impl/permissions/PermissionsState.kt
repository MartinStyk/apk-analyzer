package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionLevel

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
    val protectionFlags: ImmutableList<ProtectionFlag>,
    val grantState: GrantState?,
    val declaringPackage: String?,
    val isSelfDeclared: Boolean,
)

@Immutable
internal data class PermissionSection(val protectionLevel: ProtectionLevel?, val permissions: ImmutableList<PermissionItem>)

@Immutable
internal sealed interface PermissionsState {
    data object Loading : PermissionsState

    data object Error : PermissionsState

    @Immutable
    data class Loaded(
        val scope: PermissionScope,
        val scopeOptions: ImmutableList<PermissionScope>,
        val selectedProtectionLevels: ImmutableSet<ProtectionLevel?>,
        val protectionLevelOptions: ImmutableList<ProtectionLevel?>,
        val selectedGrantStates: ImmutableSet<GrantState>,
        val grantStateOptions: ImmutableList<GrantState>,
        val query: String,
        val scopeTotal: Int,
        val sections: ImmutableList<PermissionSection>,
    ) : PermissionsState {
        val hasScopeChoice: Boolean
            get() = scopeOptions.size > 1

        val hasResults: Boolean
            get() = sections.isNotEmpty()

        val isNarrowed: Boolean
            get() = query.isNotBlank() || selectedProtectionLevels.isNotEmpty() || selectedGrantStates.isNotEmpty()
    }
}
