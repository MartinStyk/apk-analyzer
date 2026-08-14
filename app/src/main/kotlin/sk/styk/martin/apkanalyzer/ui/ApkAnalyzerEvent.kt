package sk.styk.martin.apkanalyzer.ui

internal sealed interface ApkAnalyzerEvent {
    data object RequestReview : ApkAnalyzerEvent
}
