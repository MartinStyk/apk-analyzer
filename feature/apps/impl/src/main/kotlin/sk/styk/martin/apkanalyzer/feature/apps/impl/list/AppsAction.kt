package sk.styk.martin.apkanalyzer.feature.apps.impl.list

sealed interface AppsAction {
    data class SortTypeSelected(val sortType: SortType) : AppsAction
    data class AppClicked(val packageName: String) : AppsAction
    data object SearchClicked : AppsAction
    data object OpenSettings : AppsAction
    data object OpenApkDetails : AppsAction
}
