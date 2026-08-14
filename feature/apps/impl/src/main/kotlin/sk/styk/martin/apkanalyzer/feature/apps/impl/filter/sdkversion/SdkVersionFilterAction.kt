package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.sdkversion

sealed interface SdkVersionFilterAction {
    data class SdkVersionToggled(val sdkVersion: Int) : SdkVersionFilterAction
    data object Reset : SdkVersionFilterAction
    data object NavigateBack : SdkVersionFilterAction
}
