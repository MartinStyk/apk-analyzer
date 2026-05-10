package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.permission

sealed interface PermissionFilterEvent {
    data object NavigateBack : PermissionFilterEvent
}
