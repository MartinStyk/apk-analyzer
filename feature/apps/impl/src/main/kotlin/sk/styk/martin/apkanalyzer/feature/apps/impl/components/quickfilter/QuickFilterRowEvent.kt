package sk.styk.martin.apkanalyzer.feature.apps.impl.components.quickfilter

sealed interface QuickFilterRowEvent {
    data object NavigateToFilter : QuickFilterRowEvent
}
