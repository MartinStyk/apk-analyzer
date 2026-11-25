package sk.styk.martin.apkanalyzer.feature.appdetail.tv

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal sealed interface TvAppDetailEvent {
    data class OpenAppInfo(val packageName: PackageName) : TvAppDetailEvent
    data class OpenPlayStore(val packageName: PackageName) : TvAppDetailEvent
    data class NavigateToGeneralInfo(val appName: String) : TvAppDetailEvent
    data class NavigateToPermissions(val appName: String, val permissionName: String?) : TvAppDetailEvent
    data class NavigateToComponents(val appName: String) : TvAppDetailEvent
    data class NavigateToCertificates(val appName: String) : TvAppDetailEvent
    data class NavigateToRequirements(val appName: String) : TvAppDetailEvent
    data class NavigateToManifest(val appName: String) : TvAppDetailEvent
}
