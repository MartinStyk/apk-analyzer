package sk.styk.martin.apkanalyzer.feature.apps.impl.search

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

sealed interface AppSearchEvent {
    data class NavigateToAppDetail(val packageName: PackageName) : AppSearchEvent
    data object NavigateToFilter : AppSearchEvent
}
