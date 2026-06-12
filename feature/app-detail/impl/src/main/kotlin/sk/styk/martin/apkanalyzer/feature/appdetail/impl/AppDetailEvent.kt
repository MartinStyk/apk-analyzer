package sk.styk.martin.apkanalyzer.feature.appdetail.impl

sealed interface AppDetailEvent {
    data class OpenPlayStore(val packageName: String) : AppDetailEvent
    data class OpenAppInfo(val packageName: String) : AppDetailEvent
    data class ExportApk(val packageName: String) : AppDetailEvent
    data class SaveIcon(val packageName: String) : AppDetailEvent
    data object NavigateToManifest : AppDetailEvent
    data object NavigateToGeneralDetails : AppDetailEvent
    data object NavigateToPermissions : AppDetailEvent
    data object NavigateToActivities : AppDetailEvent
    data object NavigateToServices : AppDetailEvent
    data object NavigateToReceivers : AppDetailEvent
    data object NavigateToProviders : AppDetailEvent
    data object NavigateToCertificates : AppDetailEvent
    data object NavigateToFeatures : AppDetailEvent
}
