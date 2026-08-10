package sk.styk.martin.apkanalyzer.feature.appdetail.impl.nativelibraries

internal sealed interface NativeLibrariesEvent {
    data object NavigateBack : NativeLibrariesEvent

    data object ShowCopiedFeedback : NativeLibrariesEvent
}
