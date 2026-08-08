package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

internal sealed interface IntentFiltersAction {
    data object Retry : IntentFiltersAction
    data object Back : IntentFiltersAction
    data object ClearQuery : IntentFiltersAction
    data class ChangeQuery(val query: String) : IntentFiltersAction
    data class CopyValue(val label: String, val value: String) : IntentFiltersAction
}
