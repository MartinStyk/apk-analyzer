package sk.styk.martin.apkanalyzer.core.apps

import android.annotation.SuppressLint
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
import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.components.resolvePathPermissions
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceResolver
import sk.styk.martin.apkanalyzer.core.apps.installsource.isSystemInstalledApp
import sk.styk.martin.apkanalyzer.core.apps.installsource.resolveAppInstallSource
import sk.styk.martin.apkanalyzer.core.apps.manifest.ManifestParser
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.AppInfo
import sk.styk.martin.apkanalyzer.core.apps.model.InstallLocation
import sk.styk.martin.apkanalyzer.core.apps.packaging.computeApkSize
import sk.styk.martin.apkanalyzer.core.apps.packaging.readInstalledSplits
import sk.styk.martin.apkanalyzer.core.apps.packaging.readNativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.PermissionDetails
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permissions
import sk.styk.martin.apkanalyzer.core.apps.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.apps.permissions.resolveProtectionFlags
import sk.styk.martin.apkanalyzer.core.apps.permissions.resolveProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.sdkversion.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.apps.signing.ApkSigningBlockAnalyzer
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateExtractor
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.logger.nextOperationRequest
import sk.styk.martin.apkanalyzer.core.common.logger.operationLogMessage
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Suppress("TooManyFunctions")
@Singleton
internal class AppDetailRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val sdkVersionResolver: SdkVersionResolver,
    private val installSourceResolver: InstallSourceResolver,
    private val certificateExtractor: CertificateExtractor,
    private val apkSigningBlockAnalyzer: ApkSigningBlockAnalyzer,
    private val manifestParser: ManifestParser,
    private val storageStatsRepository: StorageStatsRepository,
    private val usageStatsRepository: UsageStatsRepository,
    packageChangesObserver: PackageChangesObserver,
    appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
) : AppDetailRepository {

    private val cache = ConcurrentHashMap<CacheKey, AppDetail>()

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

    @Suppress("SuspendFunSwallowedCancellation")
    private suspend fun installedPackageDetails(packageName: PackageName): Result<AppDetail> {
        val requestId = nextOperationRequest()
        val context = "mode=installed package=${packageName.value}"
        val cacheKey = CacheKey.InstalledPackage(packageName)
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "started", context = context))
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_CACHE_LOOKUP, event = "started", context = context))
        cache[cacheKey]?.let {
            Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_CACHE_LOOKUP, event = "succeeded", context = "cache_hit=true $context"))
            val event = if (it.areComponentIntentFiltersAvailable) "succeeded" else "degraded"
            Logger.i(TAG, operationLogMessage(OPERATION, requestId, event = event, context = "cache_hit=true $context"))
            return Result.success(it)
        }

        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_CACHE_LOOKUP, event = "succeeded", context = "cache_hit=false $context"))
        return try {
            runCatchingCancellable {
                val reference = AppReference.InstalledPackage(packageName)

                Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_PACKAGE_QUERY, event = "started", context = context))
                val packageInfo = packageManager.getPackageInfo(packageName.value, analysisFlags)
                Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_PACKAGE_QUERY, event = "succeeded", context = context))

                val intentFilters = manifestParser.componentIntentFilters(reference)
                Logger.d(
                    TAG,
                    operationLogMessage(
                        OPERATION,
                        requestId,
                        stage = STAGE_INTENT_FILTERS,
                        event = if (intentFilters.isSuccess) "succeeded" else "degraded",
                        context = context,
                    ),
                )

                val totalSize = storageStatsRepository.queryTotalSize(packageName)
                Logger.d(
                    TAG,
                    operationLogMessage(
                        OPERATION,
                        requestId,
                        stage = STAGE_STORAGE_STATS,
                        event = if (totalSize != null) "succeeded" else "degraded",
                        context = "available=${totalSize != null} $context",
                    ),
                )

                val lastUsedTime = usageStatsRepository.queryLastUsedTime(packageName)
                Logger.d(
                    TAG,
                    operationLogMessage(
                        OPERATION,
                        requestId,
                        stage = STAGE_USAGE_STATS,
                        event = if (lastUsedTime != null) "succeeded" else "degraded",
                        context = "available=${lastUsedTime != null} $context",
                    ),
                )

                getPackageDetails(
                    requestId = requestId,
                    context = context,
                    analysisMode = AppDetail.AnalysisMode.InstalledPackage,
                    packageInfo = packageInfo,
                    intentFiltersByComponent = intentFilters.getOrDefault(emptyMap()),
                    areIntentFiltersAvailable = intentFilters.isSuccess,
                    totalSize = totalSize,
                    lastUsedTime = lastUsedTime,
                )
            }.onSuccess { detail ->
                cache[cacheKey] = detail
                val event = if (detail.areComponentIntentFiltersAvailable) "succeeded" else "degraded"
                Logger.i(TAG, operationLogMessage(OPERATION, requestId, event = event, context = context))
            }.onFailure {
                Logger.e(TAG, it, operationLogMessage(OPERATION, requestId, event = "failed", context = context))
            }
        } catch (cancellation: CancellationException) {
            Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "cancelled", context = context))
            throw cancellation
        }
    }

    @Suppress("SuspendFunSwallowedCancellation")
    private suspend fun apkFilePackageDetails(accessibleFile: File): Result<AppDetail> {
        val requestId = nextOperationRequest()
        val context = "mode=apk_file apk_path=${accessibleFile.absolutePath}"
        val cacheKey = CacheKey.ApkFile(accessibleFile.absolutePath, accessibleFile.lastModified())
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "started", context = context))
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_CACHE_LOOKUP, event = "started", context = context))
        cache[cacheKey]?.let {
            Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_CACHE_LOOKUP, event = "succeeded", context = "cache_hit=true $context"))
            val event = if (it.areComponentIntentFiltersAvailable) "succeeded" else "degraded"
            Logger.i(TAG, operationLogMessage(OPERATION, requestId, event = event, context = "cache_hit=true $context"))
            return Result.success(it)
        }

        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_CACHE_LOOKUP, event = "succeeded", context = "cache_hit=false $context"))
        return try {
            runCatchingCancellable {
                val reference = AppReference.ApkFile(accessibleFile.absolutePath)

                Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_PACKAGE_QUERY, event = "started", context = context))
                val packageInfo = packageManager.getPackageArchiveInfoWithCorrectPath(accessibleFile.absolutePath, analysisFlags)
                    ?: error("Cannot parse APK file: ${accessibleFile.absolutePath}")
                Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_PACKAGE_QUERY, event = "succeeded", context = context))

                val intentFilters = manifestParser.componentIntentFilters(reference)
                Logger.d(
                    TAG,
                    operationLogMessage(
                        OPERATION,
                        requestId,
                        stage = STAGE_INTENT_FILTERS,
                        event = if (intentFilters.isSuccess) "succeeded" else "degraded",
                        context = context,
                    ),
                )

                getPackageDetails(
                    requestId = requestId,
                    context = context,
                    analysisMode = AppDetail.AnalysisMode.ApkFile,
                    packageInfo = packageInfo,
                    intentFiltersByComponent = intentFilters.getOrDefault(emptyMap()),
                    areIntentFiltersAvailable = intentFilters.isSuccess,
                )
            }.onSuccess { detail ->
                cache[cacheKey] = detail
                val event = if (detail.areComponentIntentFiltersAvailable) "succeeded" else "degraded"
                Logger.i(TAG, operationLogMessage(OPERATION, requestId, event = event, context = context))
            }.onFailure {
                Logger.e(TAG, it, operationLogMessage(OPERATION, requestId, event = "failed", context = context))
            }
        } catch (cancellation: CancellationException) {
            Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "cancelled", context = context))
            throw cancellation
        }
    }

    private fun getPackageDetails(
        requestId: Long,
        context: String,
        analysisMode: AppDetail.AnalysisMode,
        packageInfo: PackageInfo,
        intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>,
        areIntentFiltersAvailable: Boolean,
        totalSize: AppSize? = null,
        lastUsedTime: Instant? = null,
    ): AppDetail {
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_GENERAL_INFO, event = "started", context = context))
        val info = getGeneralData(packageInfo, totalSize, lastUsedTime)
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_GENERAL_INFO, event = "succeeded", context = context))

        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_CERTIFICATES, event = "started", context = context))
        val signing = certificateExtractor.getAppSigning(packageInfo)
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_CERTIFICATES, event = "succeeded", context = context))

        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_SIGNING_SCHEMES, event = "started", context = context))
        val signingSchemeVersions = packageInfo.applicationInfo?.sourceDir?.let(apkSigningBlockAnalyzer::detectSchemeVersions)
        Logger.d(
            TAG,
            operationLogMessage(
                OPERATION,
                requestId,
                stage = STAGE_SIGNING_SCHEMES,
                event = if (signingSchemeVersions != null) "succeeded" else "degraded",
                context = context,
            ),
        )

        val launcherActivityNames = when (analysisMode) {
            AppDetail.AnalysisMode.InstalledPackage -> {
                Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_LAUNCHER_QUERY, event = "started", context = context))
                queryLauncherActivityNames(PackageName(packageInfo.packageName)).also {
                    Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_LAUNCHER_QUERY, event = "succeeded", context = context))
                }
            }

            AppDetail.AnalysisMode.ApkFile -> null
        }

        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_COMPONENT_MAPPING, event = "started", context = context))
        val activities = getActivities(packageInfo, launcherActivityNames, intentFiltersByComponent)
        val services = getServices(packageInfo, intentFiltersByComponent)
        val contentProviders = getContentProviders(packageInfo, intentFiltersByComponent)
        val receivers = getBroadcastReceivers(packageInfo, intentFiltersByComponent)
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_COMPONENT_MAPPING, event = "succeeded", context = context))

        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_PERMISSIONS, event = "started", context = context))
        val permissions = getPermissions(packageInfo)
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_PERMISSIONS, event = "succeeded", context = context))

        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_FEATURES, event = "started", context = context))
        val features = getFeatures(packageInfo)
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_FEATURES, event = "succeeded", context = context))

        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_PACKAGING, event = "started", context = context))
        val nativeLibraries = readNativeLibraries(packageInfo.applicationInfo)
        Logger.d(TAG, operationLogMessage(OPERATION, requestId, stage = STAGE_PACKAGING, event = "succeeded", context = context))

        return AppDetail(
            analysisMode = analysisMode,
            info = info,
            signing = signing.copy(signingSchemeVersions = signingSchemeVersions),
            activities = activities,
            services = services,
            contentProviders = contentProviders,
            receivers = receivers,
            permissions = permissions,
            features = features,
            nativeLibraries = nativeLibraries,
            areComponentIntentFiltersAvailable = areIntentFiltersAvailable,
        )
    }

    private fun getGeneralData(
        packageInfo: PackageInfo,
        totalSize: AppSize? = null,
        lastUsedTime: Instant? = null,
    ): AppInfo {
        val applicationInfo = packageInfo.applicationInfo
        val minSdk = applicationInfo?.minSdkVersion
        val installSourceChain = installSourceResolver.resolve(packageInfo)
        val isSystemApp = isSystemInstalledApp(packageInfo)

        return AppInfo(
            packageName = PackageName(packageInfo.packageName),
            applicationName = applicationInfo?.loadLabel(packageManager)?.toString() ?: packageInfo.packageName,
            processName = applicationInfo?.processName,
            versionName = packageInfo.versionName,
            versionCode = packageInfo.longVersionCode,
            isSystemApp = isSystemApp,
            isDebuggable = applicationInfo.hasFlag(ApplicationInfo.FLAG_DEBUGGABLE),
            allowsBackup = applicationInfo.hasFlag(ApplicationInfo.FLAG_ALLOW_BACKUP),
            usesCleartextTraffic = applicationInfo.hasFlag(ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC),
            uid = applicationInfo?.uid,
            sharedUserId = packageInfo.sharedUserId,
            description = applicationInfo?.loadDescription(packageManager)?.toString(),
            source = resolveAppInstallSource(installSourceChain, isSystemApp),
            apkDirectory = applicationInfo?.sourceDir,
            dataDirectory = applicationInfo?.dataDir,
            installSourceChain = installSourceChain,
            installLocation = InstallLocation.from(packageInfo.installLocation),
            apkSize = computeApkSize(applicationInfo),
            firstInstallTime = if (packageInfo.firstInstallTime > 0) Instant.ofEpochMilli(packageInfo.firstInstallTime) else null,
            lastUpdateTime = if (packageInfo.lastUpdateTime > 0) Instant.ofEpochMilli(packageInfo.lastUpdateTime) else null,
            minSdkVersion = minSdk,
            minSdkLabel = sdkVersionResolver.resolveVersion(minSdk),
            targetSdkVersion = applicationInfo?.targetSdkVersion,
            targetSdkLabel = sdkVersionResolver.resolveVersion(applicationInfo?.targetSdkVersion),
            totalSize = totalSize,
            lastUsedTime = lastUsedTime,
            installedSplits = readInstalledSplits(applicationInfo),
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

    @SuppressLint("QueryPermissionsNeeded")
    private fun queryLauncherActivityNames(packageName: PackageName): Set<String> = launcherCategories
        .flatMap { category ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(packageName.value)
            runCatching { packageManager.queryIntentActivities(intent, 0) }
                .onFailure { Logger.w(TAG, it, "Can not resolve launcher activities of $packageName") }
                .getOrDefault(emptyList())
                .map { it.activityInfo.name }
        }
        .toSet()

    private fun getServices(packageInfo: PackageInfo, intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>): List<Service> =
        packageInfo.services.orEmpty().map {
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

    private fun getContentProviders(
        packageInfo: PackageInfo,
        intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>,
    ): List<ContentProvider> = packageInfo.providers.orEmpty().map {
        ContentProvider(
            name = it.name,
            authority = it.authority,
            readPermission = it.readPermission,
            writePermission = it.writePermission,
            isExported = it.exported,
            pathPermissions = resolvePathPermissions(it.pathPermissions),
            intentFilters = intentFiltersByComponent[ComponentIntentFilterKey(it.name, ComponentKind.Provider)].orEmpty(),
        )
    }

    private fun getBroadcastReceivers(
        packageInfo: PackageInfo,
        intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>,
    ): List<BroadcastReceiver> = packageInfo.receivers.orEmpty().map {
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

    private sealed interface CacheKey {
        data class InstalledPackage(val packageName: PackageName) : CacheKey
        data class ApkFile(val path: String, val lastModified: Long) : CacheKey
    }

    companion object {
        private const val TAG = "AppDetailRepositoryImpl"
        private const val OPERATION = "app_detail"
        private const val STAGE_CACHE_LOOKUP = "cache_lookup"
        private const val STAGE_PACKAGE_QUERY = "package_query"
        private const val STAGE_INTENT_FILTERS = "intent_filters"
        private const val STAGE_STORAGE_STATS = "storage_stats"
        private const val STAGE_USAGE_STATS = "usage_stats"
        private const val STAGE_GENERAL_INFO = "general_info"
        private const val STAGE_CERTIFICATES = "certificates"
        private const val STAGE_SIGNING_SCHEMES = "signing_schemes"
        private const val STAGE_LAUNCHER_QUERY = "launcher_query"
        private const val STAGE_COMPONENT_MAPPING = "component_mapping"
        private const val STAGE_PERMISSIONS = "permissions"
        private const val STAGE_FEATURES = "features"
        private const val STAGE_PACKAGING = "packaging"

        private val launcherCategories = listOf(Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER)
    }
}
