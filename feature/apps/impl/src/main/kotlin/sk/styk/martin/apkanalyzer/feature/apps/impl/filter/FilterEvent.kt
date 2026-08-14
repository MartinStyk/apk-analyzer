package sk.styk.martin.apkanalyzer.feature.apps.impl.filter

sealed interface FilterEvent {
    data object NavigateBack : FilterEvent
    data object OpenUsagePermissionSettings : FilterEvent
    data object NavigateToPermissionFilter : FilterEvent
    data object NavigateToSourceFilter : FilterEvent
    data object NavigateToSdkVersionFilter : FilterEvent
}
