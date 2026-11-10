package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.change

import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

internal sealed interface AppHistoryChangeEvent {
    data object NavigateBack : AppHistoryChangeEvent

    data class NavigateToFullDetail(val input: AppDetailInput.Historical) : AppHistoryChangeEvent

    data object ShowCopiedFeedback : AppHistoryChangeEvent
}
