package sk.styk.martin.apkanalyzer.feature.appdetail.tv

import sk.styk.martin.apkanalyzer.feature.appdetail.domain.insight.AppDetailInsight

internal sealed interface TvAppDetailAction {
    data object Retry : TvAppDetailAction
    data object OpenAppInfo : TvAppDetailAction
    data object OpenPlayStore : TvAppDetailAction
    data object OpenGeneralInfo : TvAppDetailAction
    data object OpenPermissions : TvAppDetailAction
    data object OpenComponents : TvAppDetailAction
    data object OpenCertificates : TvAppDetailAction
    data object OpenRequirements : TvAppDetailAction
    data object OpenManifest : TvAppDetailAction
    data class OpenInsight(val insight: AppDetailInsight) : TvAppDetailAction
}
