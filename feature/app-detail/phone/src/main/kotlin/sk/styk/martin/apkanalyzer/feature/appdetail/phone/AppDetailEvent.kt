package sk.styk.martin.apkanalyzer.feature.appdetail.phone

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal sealed interface AppDetailEvent {
    data class OpenPlayStore(val packageName: PackageName) : AppDetailEvent
    data class OpenAppInfo(val packageName: PackageName) : AppDetailEvent
    data class CreateDocument(val export: AppDetailExport, val suggestedName: String) : AppDetailEvent
    data class ShowSummaryPreview(val text: String) : AppDetailEvent
    data class ShareSummary(val text: String) : AppDetailEvent
    data class NavigateToInventoryExport(val packageName: PackageName) : AppDetailEvent
    data class ShowFeedback(val feedback: AppDetailFeedback) : AppDetailEvent
    data class NavigateToManifest(val appName: String) : AppDetailEvent
    data class NavigateToGeneralDetails(val appName: String) : AppDetailEvent
    data class NavigateToPermissions(val appName: String, val permissionName: String?) : AppDetailEvent
    data class NavigateToComponents(val appName: String) : AppDetailEvent
    data class NavigateToActivities(val appName: String) : AppDetailEvent
    data class NavigateToServices(val appName: String) : AppDetailEvent
    data class NavigateToReceivers(val appName: String) : AppDetailEvent
    data class NavigateToProviders(val appName: String) : AppDetailEvent
    data class NavigateToCertificates(val appName: String) : AppDetailEvent
    data class NavigateToFeatures(val appName: String) : AppDetailEvent
    data class NavigateToHistory(val packageName: PackageName, val appName: String) : AppDetailEvent
}

internal sealed interface AppDetailFeedback {
    data class ApkSaved(val displayName: String, val baseApkOnly: Boolean) : AppDetailFeedback
    data class IconSaved(val displayName: String) : AppDetailFeedback
    data object ApkSaveFailed : AppDetailFeedback
    data object IconSaveFailed : AppDetailFeedback
    data object DocumentPickerUnavailable : AppDetailFeedback
    data object SummaryCopied : AppDetailFeedback
    data object ShareUnavailable : AppDetailFeedback
}
