package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

internal enum class PermissionScope {
    Requested,
    Defined,
}

internal enum class ProtectionLevel {
    Dangerous,
    Signature,
    Internal,
    Normal,
}

internal enum class ProtectionFlag {
    Privileged,
    AppOp,
    Instant,
    Development,
}

internal enum class PermissionFilter {
    Dangerous,
    Granted,
    Denied,
}

internal enum class GrantState {
    Granted,
    Denied,
}

@Immutable
internal data class PermissionItem(
    val name: String,
    val label: String,
    val description: String?,
    val groupName: String?,
    val protectionLevel: ProtectionLevel,
    val protectionFlags: ImmutableList<ProtectionFlag>,
    val grantState: GrantState?,
    val declaringPackage: String?,
)

@Immutable
internal data class PermissionSection(val protectionLevel: ProtectionLevel, val permissions: ImmutableList<PermissionItem>)

@Immutable
internal sealed interface PermissionsState {
    data object Loading : PermissionsState

    data object Error : PermissionsState

    @Immutable
    data class Loaded(
        val scope: PermissionScope,
        val scopeOptions: ImmutableList<PermissionScope>,
        val activeFilters: ImmutableSet<PermissionFilter>,
        val availableFilters: ImmutableList<PermissionFilter>,
        val query: String,
        val scopeTotal: Int,
        val sections: ImmutableList<PermissionSection>,
    ) : PermissionsState {
        val hasScopeChoice: Boolean
            get() = scopeOptions.size > 1

        val hasResults: Boolean
            get() = sections.isNotEmpty()

        val isNarrowed: Boolean
            get() = query.isNotBlank() || activeFilters.isNotEmpty()
    }
}
