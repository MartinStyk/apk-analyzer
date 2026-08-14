package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.source

sealed interface SourceFilterEvent {
    data object NavigateBack : SourceFilterEvent
}
