package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.apppermissions.DevicePermissionsRepository
import sk.styk.martin.apkanalyzer.core.apppermissions.model.DevicePermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.PermissionFilterCoordinator
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.PermissionFilterDraft
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.PermissionPreset
import javax.inject.Inject

@HiltViewModel
class PermissionFilterViewModel @Inject constructor(private val permissionFilterCoordinator: PermissionFilterCoordinator, private val devicePermissionsRepository: DevicePermissionsRepository) : ViewModel() {

    private val input = permissionFilterCoordinator.consumeInput()

    private val selectedPermissions: MutableStateFlow<ImmutableSet<String>> = MutableStateFlow(input.selectedPermissions.toPersistentSet())
    private val matchMode = MutableStateFlow(if (input.matchAll) MatchMode.All else MatchMode.Any)
    private val searchQuery = MutableStateFlow("")
    private val showOnlySelected = MutableStateFlow(false)

    private val eventChannel = Channel<PermissionFilterEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val state = combine(
        selectedPermissions,
        matchMode,
        searchQuery,
        showOnlySelected,
        devicePermissionsRepository.permissions(),
    ) { selected, mode, query, selectedOnly, allPermissions ->
        val filteredPermissions = allPermissions
            .search(query)
            .filter { perm -> !selectedOnly || perm.name in selected }

        val items = filteredPermissions
            .map { perm -> PermissionItem(permission = perm, isSelected = perm.name in selected) }
            .toImmutableList()

        val presets = PermissionPreset.all.map { preset ->
            PermissionPresetState(
                preset = preset,
                isSelected = preset.permissions.isNotEmpty() && preset.permissions.all { it in selected },
            )
        }.toImmutableList()

        PermissionFilterState(
            matchMode = mode,
            searchQuery = query,
            permissionListState = PermissionListState.Permissions(items),
            presets = presets,
            showOnlySelected = selectedOnly,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PermissionFilterState())

    fun onAction(action: PermissionFilterAction) {
        when (action) {
            is PermissionFilterAction.PermissionToggled -> selectedPermissions.update { current ->
                if (action.permissionName in current) {
                    (current - action.permissionName).toPersistentSet()
                } else {
                    (current + action.permissionName).toPersistentSet()
                }
            }

            is PermissionFilterAction.PresetToggled -> selectedPermissions.update { current ->
                val preset = action.preset
                val allSelected = preset.permissions.all { it in current }
                if (allSelected) {
                    (current - preset.permissions).toPersistentSet()
                } else {
                    (current + preset.permissions).toPersistentSet()
                }
            }

            is PermissionFilterAction.SearchQueryChanged -> searchQuery.value = action.query

            is PermissionFilterAction.MatchModeChanged -> matchMode.value = action.mode

            PermissionFilterAction.ShowOnlySelectedToggled -> showOnlySelected.update { !it }

            PermissionFilterAction.Reset -> {
                selectedPermissions.value = persistentSetOf()
                matchMode.value = MatchMode.Any
                showOnlySelected.value = false
            }

            PermissionFilterAction.NavigateBack -> {
                permissionFilterCoordinator.submitResult(
                    PermissionFilterDraft(
                        selectedPermissions = selectedPermissions.value,
                        matchAll = matchMode.value == MatchMode.All,
                    ),
                )
                eventChannel.trySend(PermissionFilterEvent.NavigateBack)
            }
        }
    }

    private fun List<DevicePermission>.search(query: String) = if (query.isBlank()) {
        this
    } else {
        filter {
            it.label.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
        }
    }
}
