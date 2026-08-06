package sk.styk.martin.apkanalyzer.core.apps.model

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
    }
}
