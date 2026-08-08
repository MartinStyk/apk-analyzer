package sk.styk.martin.apkanalyzer.core.apps

import android.content.Intent
import android.content.pm.ApplicationInfo
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
import sk.styk.martin.apkanalyzer.core.apps.analysis.ManifestParser
import sk.styk.martin.apkanalyzer.core.apps.analysis.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.apps.analysis.computeApkSize
import sk.styk.martin.apkanalyzer.core.apps.analysis.resolveProtectionFlags
import sk.styk.martin.apkanalyzer.core.apps.analysis.resolveProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.model.Activity
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.AppInfo
import sk.styk.martin.apkanalyzer.core.apps.model.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.model.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.model.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.model.Feature
import sk.styk.martin.apkanalyzer.core.apps.model.InstallLocation
import sk.styk.martin.apkanalyzer.core.apps.model.Permission
import sk.styk.martin.apkanalyzer.core.apps.model.PermissionDetails
import sk.styk.martin.apkanalyzer.core.apps.model.Permissions
import sk.styk.martin.apkanalyzer.core.apps.model.Service
import sk.styk.martin.apkanalyzer.core.apps.model.UsedPermission
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
internal class AppDetailRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val sdkVersionResolver: SdkVersionResolver,
    private val installSourceResolver: InstallSourceResolver,
    private val certificateExtractor: CertificateExtractor,
    private val manifestParser: ManifestParser,
    private val storageStatsRepository: StorageStatsRepository,
    private val usageStatsRepository: UsageStatsRepository,
    packageChangesObserver: PackageChangesObserver,
    appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
) : AppDetailRepository {

    private val cache = ConcurrentHashMap<PackageName, AppDetail>()

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

    override suspend fun details(reference: AppReference): Result<AppDetail> = when (reference) {
        is AppReference.InstalledPackage -> installedPackageDetails(reference.packageName)
        is AppReference.ApkFile -> apkFilePackageDetails(File(reference.path))
    }

    private suspend fun installedPackageDetails(packageName: PackageName): Result<AppDetail> {
        cache[packageName]?.let { return Result.success(it) }
        Logger.d(TAG, "Loading details of $packageName")
        return runCatchingCancellable {
            val reference = AppReference.InstalledPackage(packageName)
            val packageInfo = packageManager.getPackageInfo(packageName.value, analysisFlags)
            val intentFilters = manifestParser.componentIntentFilters(reference)
            getPackageDetails(
                analysisMode = AppDetail.AnalysisMode.InstalledPackage,
                packageInfo = packageInfo,
                intentFiltersByComponent = intentFilters.getOrDefault(emptyMap()),
                areIntentFiltersAvailable = intentFilters.isSuccess,
                totalSize = storageStatsRepository.queryTotalSize(packageName),
                lastUsedTime = usageStatsRepository.queryLastUsedTime(packageName),
            )
        }.onSuccess { cache[packageName] = it }
    }

    private suspend fun apkFilePackageDetails(accessibleFile: File): Result<AppDetail> = runCatchingCancellable {
        val reference = AppReference.ApkFile(accessibleFile.absolutePath)
        val intentFilters = manifestParser.componentIntentFilters(reference)
        getPackageDetails(
            analysisMode = AppDetail.AnalysisMode.ApkFile,
            packageInfo = packageManager.getPackageArchiveInfoWithCorrectPath(accessibleFile.absolutePath, analysisFlags)
                ?: error("Cannot parse APK file: ${accessibleFile.absolutePath}"),
            intentFiltersByComponent = intentFilters.getOrDefault(emptyMap()),
            areIntentFiltersAvailable = intentFilters.isSuccess,
        )
    }

    private fun getPackageDetails(
        analysisMode: AppDetail.AnalysisMode,
        packageInfo: PackageInfo,
        intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>,
        areIntentFiltersAvailable: Boolean,
        totalSize: AppSize? = null,
        lastUsedTime: Instant? = null,
    ) = AppDetail(
        analysisMode = analysisMode,
        info = getGeneralData(packageInfo, totalSize, lastUsedTime),
        signing = certificateExtractor.getAppSigning(packageInfo),
        activities = getActivities(
            packageInfo = packageInfo,
            launcherActivityNames = when (analysisMode) {
                AppDetail.AnalysisMode.InstalledPackage -> queryLauncherActivityNames(PackageName(packageInfo.packageName))
                AppDetail.AnalysisMode.ApkFile -> null
            },
            intentFiltersByComponent = intentFiltersByComponent,
        ),
        services = getServices(packageInfo, intentFiltersByComponent),
        contentProviders = getContentProviders(packageInfo, intentFiltersByComponent),
        receivers = getBroadcastReceivers(packageInfo, intentFiltersByComponent),
        permissions = getPermissions(packageInfo),
        features = getFeatures(packageInfo),
        areComponentIntentFiltersAvailable = areIntentFiltersAvailable,
    )

    private fun getGeneralData(
        packageInfo: PackageInfo,
        totalSize: AppSize? = null,
        lastUsedTime: Instant? = null,
    ): AppInfo {
        val applicationInfo = packageInfo.applicationInfo
        val minSdk = applicationInfo?.minSdkVersion

        return AppInfo(
            packageName = PackageName(packageInfo.packageName),
            applicationName = applicationInfo?.loadLabel(packageManager)?.toString() ?: packageInfo.packageName,
            processName = applicationInfo?.processName,
            versionName = packageInfo.versionName,
            versionCode = packageInfo.longVersionCode,
            isSystemApp = installSourceResolver.isSystemInstalledApp(packageInfo),
            isDebuggable = applicationInfo.hasFlag(ApplicationInfo.FLAG_DEBUGGABLE),
            allowsBackup = applicationInfo.hasFlag(ApplicationInfo.FLAG_ALLOW_BACKUP),
            usesCleartextTraffic = applicationInfo.hasFlag(ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC),
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

    private fun getActivities(
        packageInfo: PackageInfo,
        launcherActivityNames: Set<String>?,
        intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>,
    ): List<Activity> = packageInfo.activities.orEmpty().map {
        Activity(
            name = it.name,
            packageName = PackageName(it.packageName),
            label = it.loadLabel(packageManager).toString(),
            targetActivity = it.targetActivity,
            permission = it.permission,
            parentName = it.parentActivityName,
            isExported = it.exported,
            isLauncher = launcherActivityNames?.contains(it.name),
            intentFilters = intentFiltersByComponent[ComponentIntentFilterKey(it.name, ComponentKind.Activity)].orEmpty(),
        )
    }

    private fun queryLauncherActivityNames(packageName: PackageName): Set<String> = launcherCategories
        .flatMap { category ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(packageName.value)
            runCatching { packageManager.queryIntentActivities(intent, 0) }
                .onFailure { Logger.w(TAG, "Can not resolve launcher activities of $packageName") }
                .getOrDefault(emptyList())
                .map { it.activityInfo.name }
        }
        .toSet()

    private fun getServices(packageInfo: PackageInfo, intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>): List<Service> = packageInfo.services.orEmpty().map {
        Service(
            name = it.name,
            permission = it.permission,
            isExported = it.exported,
            isStopWithTask = it.flags and ServiceInfo.FLAG_STOP_WITH_TASK > 0,
            isSingleUser = it.flags and ServiceInfo.FLAG_SINGLE_USER > 0,
            isIsolatedProcess = it.flags and ServiceInfo.FLAG_ISOLATED_PROCESS > 0,
            isExternalService = it.flags and ServiceInfo.FLAG_EXTERNAL_SERVICE > 0,
            intentFilters = intentFiltersByComponent[ComponentIntentFilterKey(it.name, ComponentKind.Service)].orEmpty(),
        )
    }

    private fun getContentProviders(packageInfo: PackageInfo, intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>): List<ContentProvider> = packageInfo.providers.orEmpty().map {
        ContentProvider(
            name = it.name,
            authority = it.authority,
            readPermission = it.readPermission,
            writePermission = it.writePermission,
            isExported = it.exported,
            intentFilters = intentFiltersByComponent[ComponentIntentFilterKey(it.name, ComponentKind.Provider)].orEmpty(),
        )
    }

    private fun getBroadcastReceivers(packageInfo: PackageInfo, intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>): List<BroadcastReceiver> = packageInfo.receivers.orEmpty().map {
        BroadcastReceiver(
            name = it.name,
            permission = it.permission,
            isExported = it.exported,
            intentFilters = intentFiltersByComponent[ComponentIntentFilterKey(it.name, ComponentKind.Receiver)].orEmpty(),
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
            details = it.toDetails(),
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
                    details = permissionInfo?.toDetails(),
                ),
                isGranted = (grantedFlags?.getOrNull(index) ?: 0) and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0,
            )
        }
    }

    private fun PermissionInfo.toDetails() = PermissionDetails(
        groupName = group,
        protectionLevel = resolveProtectionLevel(protection),
        protectionFlags = resolveProtectionFlags(protectionFlags),
        description = loadDescription(packageManager)?.toString(),
        declaringPackage = PackageName(packageName),
    )

    private fun getFeatures(packageInfo: PackageInfo): List<Feature> = packageInfo.reqFeatures.orEmpty().map { featureInfo ->
        val isRequired = (featureInfo.flags and FeatureInfo.FLAG_REQUIRED) > 0
        when (val name = featureInfo.name) {
            null -> Feature.OpenGlEs(reqGlEsVersion = featureInfo.reqGlEsVersion, isRequired = isRequired)
            else -> Feature.Hardware(name = name, version = featureInfo.version, isRequired = isRequired)
        }
    }

    private fun ApplicationInfo?.hasFlag(flag: Int): Boolean = this?.flags?.and(flag) != 0

    private fun PackageManager.getPackageArchiveInfoWithCorrectPath(pathToPackage: String, flags: Int): PackageInfo? {
        val packageInfo = getPackageArchiveInfo(pathToPackage, flags)
        packageInfo?.applicationInfo?.sourceDir = pathToPackage
        packageInfo?.applicationInfo?.publicSourceDir = pathToPackage
        return packageInfo
    }

    companion object {
        private const val TAG = "AppDetailRepositoryImpl"

        private val launcherCategories = listOf(Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER)
    }
}
