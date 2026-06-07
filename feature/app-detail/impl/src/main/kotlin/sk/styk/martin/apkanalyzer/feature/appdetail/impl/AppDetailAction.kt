package sk.styk.martin.apkanalyzer.feature.appdetail.impl

sealed interface AppDetailAction {
    data object Retry : AppDetailAction
}
