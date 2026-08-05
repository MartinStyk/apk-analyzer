package sk.styk.martin.apkanalyzer.core.apps

import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
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
import sk.styk.martin.apkanalyzer.core.apps.model.Activity
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.AppInfo
import sk.styk.martin.apkanalyzer.core.apps.model.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.model.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.model.Feature
import sk.styk.martin.apkanalyzer.core.apps.model.InstallLocation
import sk.styk.martin.apkanalyzer.core.apps.model.Permission
import sk.styk.martin.apkanalyzer.core.apps.model.Permissions
import sk.styk.martin.apkanalyzer.core.apps.model.Service
import sk.styk.martin.apkanalyzer.core.apps.model.UsedPermission
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppDetailRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val sdkVersionResolver: SdkVersionResolver,
    private val installSourceResolver: InstallSourceResolver,
    private val certificateExtractor: CertificateExtractor,
    private val storageStatsRepository: StorageStatsRepository,
    private val usageStatsRepository: UsageStatsRepository,
    packageChangesObserver: PackageChangesObserver,
    appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
) : AppDetailRepository {

    private val cache = ConcurrentHashMap<String, AppDetail>()

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

    override suspend fun installedPackageDetails(packageName: String): Result<AppDetail> {
        cache[packageName]?.let { return Result.success(it) }
        Logger.d(TAG, "Loading details of $packageName")
        return runCatching {
            val packageInfo = packageManager.getPackageInfo(packageName, analysisFlags)
            getPackageDetails(
                analysisMode = AppDetail.AnalysisMode.InstalledPackage,
                packageInfo = packageInfo,
                totalSize = storageStatsRepository.queryTotalSize(packageName),
                lastUsedTime = usageStatsRepository.queryLastUsedTime(packageName),
            )
        }.onSuccess { cache[packageName] = it }
    }

    override suspend fun apkFilePackageDetails(accessibleFile: File): Result<AppDetail> = runCatching {
        getPackageDetails(
            analysisMode = AppDetail.AnalysisMode.ApkFile,
            packageInfo = packageManager.getPackageArchiveInfoWithCorrectPath(accessibleFile.absolutePath, analysisFlags)
                ?: error("Cannot parse APK file: ${accessibleFile.absolutePath}"),
        )
    }

    private fun getPackageDetails(
        analysisMode: AppDetail.AnalysisMode,
        packageInfo: PackageInfo,
        totalSize: AppSize? = null,
        lastUsedTime: Instant? = null,
    ) = AppDetail(
        analysisMode = analysisMode,
        info = getGeneralData(packageInfo, totalSize, lastUsedTime),
        certificates = certificateExtractor.getCertificateData(packageInfo),
        activities = getActivities(packageInfo),
        services = getServices(packageInfo),
        contentProviders = getContentProviders(packageInfo),
        receivers = getBroadcastReceivers(packageInfo),
        permissions = getPermissions(packageInfo),
        features = getFeatures(packageInfo),
    )

    private fun getGeneralData(
        packageInfo: PackageInfo,
        totalSize: AppSize? = null,
        lastUsedTime: Instant? = null,
    ): AppInfo {
        val applicationInfo = packageInfo.applicationInfo
        val minSdk = applicationInfo?.minSdkVersion

        return AppInfo(
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
            firstInstallTime = if (packageInfo.firstInstallTime > 0) Instant.ofEpochMilli(packageInfo.firstInstallTime) else null,
            lastUpdateTime = if (packageInfo.lastUpdateTime > 0) Instant.ofEpochMilli(packageInfo.lastUpdateTime) else null,
            minSdkVersion = minSdk,
            minSdkLabel = sdkVersionResolver.resolveVersion(minSdk),
            targetSdkVersion = applicationInfo?.targetSdkVersion,
            targetSdkLabel = sdkVersionResolver.resolveVersion(applicationInfo?.targetSdkVersion),
            totalSize = totalSize,
            lastUsedTime = lastUsedTime,
        )
    }

    private fun getActivities(packageInfo: PackageInfo): List<Activity> = packageInfo.activities.orEmpty().map {
        Activity(
            name = it.name,
            packageName = it.packageName,
            label = it.loadLabel(packageManager).toString(),
            targetActivity = it.targetActivity,
            permission = it.permission,
            parentName = it.parentActivityName,
            isExported = it.exported,
        )
    }

    private fun getServices(packageInfo: PackageInfo): List<Service> = packageInfo.services.orEmpty().map {
        Service(
            name = it.name,
            permission = it.permission,
            isExported = it.exported,
            isStopWithTask = it.flags and ServiceInfo.FLAG_STOP_WITH_TASK > 0,
            isSingleUser = it.flags and ServiceInfo.FLAG_SINGLE_USER > 0,
            isIsolatedProcess = it.flags and ServiceInfo.FLAG_ISOLATED_PROCESS > 0,
            isExternalService = it.flags and ServiceInfo.FLAG_EXTERNAL_SERVICE > 0,
        )
    }

    private fun getContentProviders(packageInfo: PackageInfo): List<ContentProvider> = packageInfo.providers.orEmpty().map {
        ContentProvider(
            name = it.name,
            authority = it.authority,
            readPermission = it.readPermission,
            writePermission = it.writePermission,
            isExported = it.exported,
        )
    }

    private fun getBroadcastReceivers(packageInfo: PackageInfo): List<BroadcastReceiver> = packageInfo.receivers.orEmpty().map {
        BroadcastReceiver(
            name = it.name,
            permission = it.permission,
            isExported = it.exported,
        )
    }

    private fun getPermissions(packageInfo: PackageInfo): Permissions {
        val definedPermissions = getDefinedPermissions(packageInfo)
        val requestedPermissions = getUsedPermissions(packageInfo)
        return Permissions(definedPermissions, requestedPermissions)
    }

    private fun getDefinedPermissions(packageInfo: PackageInfo): List<Permission> = packageInfo.permissions.orEmpty().map {
        Permission(
            name = it.name,
            simpleName = createSimpleName(it.name),
            groupName = it.group,
            protectionLevel = resolveProtectionLevel(it.protection),
            protectionFlags = resolveProtectionFlags(it.protectionFlags),
            description = it.loadDescription(packageManager)?.toString(),
            declaringPackage = it.packageName,
        )
    }

    private fun getUsedPermissions(packageInfo: PackageInfo): List<UsedPermission> {
        val grantedFlags = packageInfo.requestedPermissionsFlags
        return packageInfo.requestedPermissions.orEmpty().mapIndexed { index, name ->
            val permissionInfo = try {
                packageManager.getPermissionInfo(name, PackageManager.GET_META_DATA)
            } catch (_: Exception) {
                null
            }
            UsedPermission(
                permissionData = Permission(
                    name = name,
                    simpleName = createSimpleName(name),
                    groupName = permissionInfo?.group,
                    protectionLevel = permissionInfo?.protection?.let(::resolveProtectionLevel) ?: ProtectionLevel.Normal,
                    protectionFlags = permissionInfo?.protectionFlags?.let(::resolveProtectionFlags).orEmpty(),
                    description = permissionInfo?.loadDescription(packageManager)?.toString(),
                    declaringPackage = permissionInfo?.packageName,
                ),
                isGranted = (grantedFlags?.getOrNull(index) ?: 0) and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0,
            )
        }
    }

    private fun getFeatures(packageInfo: PackageInfo): List<Feature> = packageInfo.reqFeatures.orEmpty().map {
        Feature(
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
