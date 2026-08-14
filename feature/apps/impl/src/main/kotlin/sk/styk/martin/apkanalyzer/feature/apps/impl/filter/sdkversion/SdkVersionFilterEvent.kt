package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.sdkversion

sealed interface SdkVersionFilterEvent {
    data object NavigateBack : SdkVersionFilterEvent
}
