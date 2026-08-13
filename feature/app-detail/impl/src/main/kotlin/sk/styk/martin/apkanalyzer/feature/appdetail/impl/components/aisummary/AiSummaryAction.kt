package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.aisummary

internal sealed interface AiSummaryAction {
    data object DownloadModel : AiSummaryAction
}
