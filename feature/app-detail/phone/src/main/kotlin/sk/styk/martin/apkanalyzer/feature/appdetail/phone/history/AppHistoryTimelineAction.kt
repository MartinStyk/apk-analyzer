package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

internal sealed interface AppHistoryTimelineAction {
    data object Back : AppHistoryTimelineAction

    data object Retry : AppHistoryTimelineAction

    data class OpenRow(val row: AppHistoryRow, val appName: String) : AppHistoryTimelineAction

    data class CopyRow(val label: String, val value: String) : AppHistoryTimelineAction
}
