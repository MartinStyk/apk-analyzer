package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

internal sealed interface PermissionsEvent {
    data object ShowCopiedFeedback : PermissionsEvent
}
