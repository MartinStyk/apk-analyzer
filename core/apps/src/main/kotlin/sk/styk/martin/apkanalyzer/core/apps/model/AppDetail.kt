package sk.styk.martin.apkanalyzer.core.apps.model

import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permissions
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning

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
    val hasNativeLibraries: Boolean,
) {
    enum class AnalysisMode {
        InstalledPackage,
        ApkFile,
    }
}
