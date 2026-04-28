package sk.styk.martin.apkanalyzer.feature.apps.impl.filter

sealed interface FilterEvent {
    data object NavigateBack : FilterEvent
}
