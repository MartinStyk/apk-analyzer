package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.timeline

import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

internal sealed interface AppHistoryTimelineEvent {
    data object NavigateBack : AppHistoryTimelineEvent

    data class NavigateToChange(val snapshotId: Long, val appName: String) : AppHistoryTimelineEvent

    data class NavigateToFullDetail(val input: AppDetailInput.Historical) : AppHistoryTimelineEvent

    data object ShowCopiedFeedback : AppHistoryTimelineEvent
}
