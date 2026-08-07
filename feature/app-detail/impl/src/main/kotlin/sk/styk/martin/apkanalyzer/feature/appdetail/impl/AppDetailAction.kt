package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import android.net.Uri

internal sealed interface AppDetailAction {
    data object Retry : AppDetailAction
    data object ViewManifest : AppDetailAction
    data object ExportApk : AppDetailAction
    data object SaveIcon : AppDetailAction
    data object OpenPlayStore : AppDetailAction
    data object OpenAppInfo : AppDetailAction
    data object NavigateGeneralDetails : AppDetailAction
    data class NavigatePermissions(val permissionName: String? = null) : AppDetailAction
    data object NavigateComponents : AppDetailAction
    data object NavigateActivities : AppDetailAction
    data object NavigateServices : AppDetailAction
    data object NavigateReceivers : AppDetailAction
    data object NavigateProviders : AppDetailAction
    data object NavigateCertificates : AppDetailAction
    data object NavigateFeatures : AppDetailAction
    data class NavigateInsight(val insight: AppDetailInsight) : AppDetailAction
    data class ExportApkTo(val destination: Uri) : AppDetailAction
    data class SaveIconTo(val destination: Uri) : AppDetailAction
    data class DocumentPickerUnavailable(val export: AppDetailExport) : AppDetailAction
    data class DocumentPickerCancelled(val export: AppDetailExport) : AppDetailAction
}
