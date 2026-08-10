package sk.styk.martin.apkanalyzer.feature.appdetail.impl.splitapks

internal sealed interface SplitApksAction {
    data object Retry : SplitApksAction

    data object Back : SplitApksAction

    data class ChangeQuery(val query: String) : SplitApksAction

    data object ClearQuery : SplitApksAction

    data class CopyValue(val label: String, val value: String) : SplitApksAction
}
