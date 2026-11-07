package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

internal sealed interface AppHistoryChangeAction {
    data object Back : AppHistoryChangeAction

    data object Retry : AppHistoryChangeAction

    data object OpenFullDetail : AppHistoryChangeAction

    data class CopyValue(val label: String, val value: String) : AppHistoryChangeAction
}
