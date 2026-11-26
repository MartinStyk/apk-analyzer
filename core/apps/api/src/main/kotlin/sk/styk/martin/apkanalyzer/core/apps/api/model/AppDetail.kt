package sk.styk.martin.apkanalyzer.core.apps.api.model

import sk.styk.martin.apkanalyzer.core.apps.api.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.api.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.api.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.api.components.Service
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.Permissions
import sk.styk.martin.apkanalyzer.core.apps.api.signing.AppSigning

data class AppDetail(
    val analysisMode: AnalysisMode,
    val info: AppInfo,
    val signing: AppSigning,
    val activities: List<Activity>,
    val services: List<Service>,
    val contentProviders: List<ContentProvider>,
    val receivers: List<BroadcastReceiver>,
    val permissions: Permissions,
    val features: List<Feature>,
) {
    enum class AnalysisMode {
        InstalledPackage,
        ApkFile,
        Historical,
    }
}
