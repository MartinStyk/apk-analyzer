package sk.styk.martin.apkanalyzer.core.apps

import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import sk.styk.martin.apkanalyzer.core.apps.analysis.CertificateExtractor
import sk.styk.martin.apkanalyzer.core.apps.analysis.InstallSourceResolver
import sk.styk.martin.apkanalyzer.core.apps.analysis.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.apps.analysis.computeApkSize
import sk.styk.martin.apkanalyzer.core.apps.analysis.createSimpleName
import sk.styk.martin.apkanalyzer.core.apps.model.ActivityData
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetailData
import sk.styk.martin.apkanalyzer.core.apps.model.BroadcastReceiverData
import sk.styk.martin.apkanalyzer.core.apps.model.ContentProviderData
import sk.styk.martin.apkanalyzer.core.apps.model.FeatureData
import sk.styk.martin.apkanalyzer.core.apps.model.GeneralData
import sk.styk.martin.apkanalyzer.core.apps.model.InstallLocation
import sk.styk.martin.apkanalyzer.core.apps.model.PermissionData
import sk.styk.martin.apkanalyzer.core.apps.model.PermissionDataAggregate
import sk.styk.martin.apkanalyzer.core.apps.model.ServiceData
import sk.styk.martin.apkanalyzer.core.apps.model.UsedPermissionData
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppDetailRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val sdkVersionResolver: SdkVersionResolver,
    private val installSourceResolver: InstallSourceResolver,
    private val certificateExtractor: CertificateExtractor,
    packageChangesObserver: PackageChangesObserver,
    appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
) : AppDetailRepository {

    private val cache = ConcurrentHashMap<String, AppDetailData>()

    private val analysisFlags = PackageManager.GET_SIGNING_CERTIFICATES or
        PackageManager.GET_ACTIVITIES or
        PackageManager.GET_SERVICES or
        PackageManager.GET_PROVIDERS or
        PackageManager.GET_RECEIVERS or
        PackageManager.GET_PERMISSIONS or
        PackageManager.GET_CONFIGURATIONS

    init {
        packageChangesObserver.observe()
            .onEach {
                Logger.d(TAG, "Package change detected, clearing cache")
                cache.clear()
            }
            .launchIn(appScope + dispatcherProvider.default())
    }

    override fun installedPackageDetails(packageName: String): Result<AppDetailData> {
        cache[packageName]?.let { return Result.success(it) }
        Logger.d(TAG, "Loading details of $packageName")
        return runCatching {
            getPackageDetails(
                analysisMode = AppDetailData.AnalysisMode.INSTALLED_PACKAGE,
                packageInfo = packageManager.getPackageInfo(packageName, analysisFlags),
            )
        }.onSuccess { cache[packageName] = it }
    }

    override fun apkFilePackageDetails(accessibleFile: File): Result<AppDetailData> = runCatching {
        getPackageDetails(
            analysisMode = AppDetailData.AnalysisMode.APK_FILE,
            packageInfo = packageManager.getPackageArchiveInfoWithCorrectPath(accessibleFile.absolutePath, analysisFlags)
                ?: error("Cannot parse APK file: ${accessibleFile.absolutePath}"),
        )
    }

    private fun getPackageDetails(analysisMode: AppDetailData.AnalysisMode, packageInfo: PackageInfo) = AppDetailData(
        analysisMode = analysisMode,
        generalData = getGeneralData(packageInfo),
        certificateData = certificateExtractor.getCertificateData(packageInfo),
        activityData = getActivities(packageInfo),
        serviceData = getServices(packageInfo),
        contentProviderData = getContentProviders(packageInfo),
        broadcastReceiverData = getBroadcastReceivers(packageInfo),
        permissionData = getPermissions(packageInfo),
        featureData = getFeatures(packageInfo),
    )

    private fun getGeneralData(packageInfo: PackageInfo): GeneralData {
        val applicationInfo = packageInfo.applicationInfo
        val minSdk = applicationInfo?.minSdkVersion

        return GeneralData(
            packageName = packageInfo.packageName,
            applicationName = applicationInfo?.loadLabel(packageManager).toString(),
            processName = applicationInfo?.processName,
            versionName = packageInfo.versionName,
            versionCode = packageInfo.longVersionCode,
            isSystemApp = installSourceResolver.isSystemInstalledApp(packageInfo),
            uid = applicationInfo?.uid,
            description = applicationInfo?.loadDescription(packageManager)?.toString(),
            apkDirectory = applicationInfo?.sourceDir,
            dataDirectory = applicationInfo?.dataDir,
            source = installSourceResolver.getAppInstallSource(packageInfo),
            appInstaller = installSourceResolver.appInstallingPackage(packageInfo),
            installLocation = InstallLocation.from(packageInfo.installLocation),
            apkSize = computeApkSize(applicationInfo?.sourceDir),
            firstInstallTime = if (packageInfo.firstInstallTime > 0) packageInfo.firstInstallTime else null,
            lastUpdateTime = if (packageInfo.lastUpdateTime > 0) packageInfo.lastUpdateTime else null,
            minSdkVersion = minSdk,
            minSdkLabel = sdkVersionResolver.resolveVersion(minSdk),
            targetSdkVersion = applicationInfo?.targetSdkVersion,
            targetSdkLabel = sdkVersionResolver.resolveVersion(applicationInfo?.targetSdkVersion),
            icon = applicationInfo?.loadIcon(packageManager),
        )
    }

    private fun getActivities(packageInfo: PackageInfo): List<ActivityData> = packageInfo.activities.orEmpty().map {
        ActivityData(
            name = it.name,
            packageName = it.packageName,
            label = it.loadLabel(packageManager).toString(),
            targetActivity = it.targetActivity,
            permission = it.permission,
            parentName = it.parentActivityName,
            isExported = it.exported,
        )
    }

    private fun getServices(packageInfo: PackageInfo): List<ServiceData> = packageInfo.services.orEmpty().map {
        ServiceData(
            name = it.name,
            permission = it.permission,
            isExported = it.exported,
            isStopWithTask = it.flags and ServiceInfo.FLAG_STOP_WITH_TASK > 0,
            isSingleUser = it.flags and ServiceInfo.FLAG_SINGLE_USER > 0,
            isIsolatedProcess = it.flags and ServiceInfo.FLAG_ISOLATED_PROCESS > 0,
            isExternalService = it.flags and ServiceInfo.FLAG_EXTERNAL_SERVICE > 0,
        )
    }

    private fun getContentProviders(packageInfo: PackageInfo): List<ContentProviderData> = packageInfo.providers.orEmpty().map {
        ContentProviderData(
            name = it.name,
            authority = it.authority,
            readPermission = it.readPermission,
            writePermission = it.writePermission,
            isExported = it.exported,
        )
    }

    private fun getBroadcastReceivers(packageInfo: PackageInfo): List<BroadcastReceiverData> = packageInfo.receivers.orEmpty().map {
        BroadcastReceiverData(
            name = it.name,
            permission = it.permission,
            isExported = it.exported,
        )
    }

    private fun getPermissions(packageInfo: PackageInfo): PermissionDataAggregate {
        val definedPermissions = getDefinedPermissions(packageInfo)
        val requestedPermissions = getUsedPermissions(packageInfo)
        return PermissionDataAggregate(definedPermissions, requestedPermissions)
    }

    private fun getDefinedPermissions(packageInfo: PackageInfo): List<PermissionData> = packageInfo.permissions.orEmpty().map {
        PermissionData(
            name = it.name,
            simpleName = createSimpleName(it.name),
            groupName = it.group,
            protectionLevel = it.protectionLevel,
        )
    }

    private fun getUsedPermissions(packageInfo: PackageInfo): List<UsedPermissionData> {
        val requestedPermissionNames = packageInfo.requestedPermissions.orEmpty()
        val requestedPermissionFlags = packageInfo.requestedPermissionsFlags
        val requestedPermissions = ArrayList<UsedPermissionData>(requestedPermissionNames.size)

        requestedPermissionNames.forEachIndexed { index, name ->
            val isGranted = ((requestedPermissionFlags?.getOrNull(index) ?: 0) and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0)

            val permissionData =
                try {
                    val permissionInfo = packageManager.getPermissionInfo(name, PackageManager.GET_META_DATA)
                    PermissionData(
                        name = name,
                        simpleName = createSimpleName(name),
                        groupName = permissionInfo.group,
                        protectionLevel = permissionInfo.protectionLevel,
                    )
                } catch (_: Exception) {
                    PermissionData(
                        name = name,
                        simpleName = createSimpleName(name),
                    )
                }

            requestedPermissions.add(UsedPermissionData(permissionData, isGranted))
        }
        return requestedPermissions
    }

    private fun getFeatures(packageInfo: PackageInfo): List<FeatureData> = packageInfo.reqFeatures.orEmpty().map {
        FeatureData(
            name = it.name ?: it.glEsVersion,
            isRequired = (it.flags and FeatureInfo.FLAG_REQUIRED) > 0,
        )
    }

    private fun PackageManager.getPackageArchiveInfoWithCorrectPath(pathToPackage: String, flags: Int): PackageInfo? {
        val packageInfo = getPackageArchiveInfo(pathToPackage, flags)
        packageInfo?.applicationInfo?.sourceDir = pathToPackage
        return packageInfo
    }

    companion object {
        private const val TAG = "AppDetailRepositoryImpl"
    }
}
