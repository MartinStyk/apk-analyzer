package sk.styk.martin.apkanalyzer.feature.settings.phone.flags

internal sealed interface FeatureFlagsEvent {
    data object NavigateBack : FeatureFlagsEvent
    data object SaveFailed : FeatureFlagsEvent
}
