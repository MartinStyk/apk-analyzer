package sk.styk.martin.apkanalyzer.core.appanalysis

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import sk.styk.martin.apkanalyzer.core.appanalysis.model.AppDetailData
import java.io.File
import javax.inject.Inject

class AppDetailDataManager
@Inject
internal constructor(
    private val packageManager: PackageManager,
    private val appPermissionManager: AppPermissionManager,
    private val featuresManager: FeaturesManager,
    private val appGeneralDataManager: AppGeneralDataManager,
    private val certificateManager: CertificateManager,
    private val appComponentsManager: AppComponentsManager,
) {
    private val analysisFlags =
        PackageManager.GET_SIGNING_CERTIFICATES or
            PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or
            PackageManager.GET_PROVIDERS or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PERMISSIONS or
            PackageManager.GET_CONFIGURATIONS

    fun installedPackageDetails(packageName: String) = getPackageDetails(
        analysisMode = AppDetailData.AnalysisMode.INSTALLED_PACKAGE,
        packageInfo = packageManager.getPackageInfo(packageName, analysisFlags),
    )

    fun apkFilePackageDetails(accessibleFile: File): AppDetailData = getPackageDetails(
        analysisMode = AppDetailData.AnalysisMode.APK_FILE,
        packageInfo =
        packageManager.getPackageArchiveInfoWithCorrectPath(accessibleFile.absolutePath, analysisFlags)
            ?: throw IllegalArgumentException(),
    )

    private fun getPackageDetails(analysisMode: AppDetailData.AnalysisMode, packageInfo: PackageInfo) = AppDetailData(
        analysisMode = analysisMode,
        generalData = appGeneralDataManager.get(packageInfo),
        certificateData = certificateManager.getCertificateData(packageInfo),
        activityData = appComponentsManager.getActivities(packageInfo),
        serviceData = appComponentsManager.getServices(packageInfo),
        contentProviderData = appComponentsManager.getContentProviders(packageInfo),
        broadcastReceiverData = appComponentsManager.getBroadcastReceivers(packageInfo),
        permissionData = appPermissionManager.getPermissions(packageInfo),
        featureData = featuresManager.getFeatures(packageInfo),
    )

    private fun PackageManager.getPackageArchiveInfoWithCorrectPath(pathToPackage: String, analysisFlags: Int): PackageInfo? {
        val packageInfo = getPackageArchiveInfo(pathToPackage, analysisFlags)
        packageInfo?.applicationInfo?.sourceDir = pathToPackage

        return packageInfo
    }
}
