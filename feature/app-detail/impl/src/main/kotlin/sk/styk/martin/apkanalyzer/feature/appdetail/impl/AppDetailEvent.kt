package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal sealed interface AppDetailEvent {
    data class OpenPlayStore(val packageName: PackageName) : AppDetailEvent
    data class OpenAppInfo(val packageName: PackageName) : AppDetailEvent
    data class CreateDocument(val export: AppDetailExport, val suggestedName: String) : AppDetailEvent
    data class ShareSummary(val text: String) : AppDetailEvent
    data class ShowFeedback(val feedback: AppDetailFeedback) : AppDetailEvent
    data object NavigateToManifest : AppDetailEvent
    data object NavigateToGeneralDetails : AppDetailEvent
    data class NavigateToPermissions(val permissionName: String?) : AppDetailEvent
    data object NavigateToComponents : AppDetailEvent
    data object NavigateToActivities : AppDetailEvent
    data object NavigateToServices : AppDetailEvent
    data object NavigateToReceivers : AppDetailEvent
    data object NavigateToProviders : AppDetailEvent
    data object NavigateToCertificates : AppDetailEvent
    data object NavigateToFeatures : AppDetailEvent
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
