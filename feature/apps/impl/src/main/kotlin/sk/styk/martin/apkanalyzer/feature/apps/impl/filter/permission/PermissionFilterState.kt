package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.permission

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apppermissions.model.DevicePermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.PermissionPreset

@Immutable
data class PermissionFilterState(
    val permissionListState: PermissionListState = PermissionListState.Loading,
    val searchQuery: String = "",
    val matchMode: MatchMode = MatchMode.Any,
    val presets: ImmutableList<PermissionPresetState> = persistentListOf(),
    val showOnlySelected: Boolean = false,
)

@Immutable
data class PermissionPresetState(val preset: PermissionPreset, val isSelected: Boolean)

enum class MatchMode { Any, All }

sealed interface PermissionListState {
    data object Loading : PermissionListState
    data class Permissions(val items: ImmutableList<PermissionItem>) : PermissionListState
}

@Immutable
data class PermissionItem(val permission: DevicePermission, val isSelected: Boolean)
