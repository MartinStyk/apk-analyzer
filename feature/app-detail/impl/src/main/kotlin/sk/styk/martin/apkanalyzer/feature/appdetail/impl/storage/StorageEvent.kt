package sk.styk.martin.apkanalyzer.feature.appdetail.impl.storage

internal sealed interface StorageEvent {
    data object NavigateBack : StorageEvent

    data object ShowCopiedFeedback : StorageEvent

    data object OpenUsagePermissionSettings : StorageEvent
}
