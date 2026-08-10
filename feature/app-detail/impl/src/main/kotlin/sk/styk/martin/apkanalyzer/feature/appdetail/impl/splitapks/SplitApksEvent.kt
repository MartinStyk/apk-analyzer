package sk.styk.martin.apkanalyzer.feature.appdetail.impl.splitapks

internal sealed interface SplitApksEvent {
    data object NavigateBack : SplitApksEvent

    data object ShowCopiedFeedback : SplitApksEvent
}
