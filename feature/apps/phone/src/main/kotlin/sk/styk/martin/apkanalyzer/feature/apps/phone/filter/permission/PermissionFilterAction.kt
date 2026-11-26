package sk.styk.martin.apkanalyzer.feature.apps.phone.filter.permission

import sk.styk.martin.apkanalyzer.core.apps.api.filtering.PermissionPreset

sealed interface PermissionFilterAction {
    data class PermissionToggled(val permissionName: String) : PermissionFilterAction
    data class PresetToggled(val preset: PermissionPreset) : PermissionFilterAction
    data class SearchQueryChanged(val query: String) : PermissionFilterAction
    data class MatchModeChanged(val mode: MatchMode) : PermissionFilterAction
    data object ShowOnlySelectedToggled : PermissionFilterAction
    data object NavigateBack : PermissionFilterAction
    data object Reset : PermissionFilterAction
}
