package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

internal sealed interface IntentFiltersEvent {
    data object NavigateBack : IntentFiltersEvent
    data object ShowCopiedFeedback : IntentFiltersEvent
}
