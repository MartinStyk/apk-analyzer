package sk.styk.martin.apkanalyzer.core.apps

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.components.resolvePathPermissions
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceResolver
import sk.styk.martin.apkanalyzer.core.apps.installsource.isSystemInstalledApp
import sk.styk.martin.apkanalyzer.core.apps.installsource.resolveAppInstallSource
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.AppInfo
import sk.styk.martin.apkanalyzer.core.apps.model.InstallLocation
import sk.styk.martin.apkanalyzer.core.apps.packaging.InstalledSplitApk
import sk.styk.martin.apkanalyzer.core.apps.packaging.SplitApkKind
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.PermissionDefinitionResolver
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permissions
import sk.styk.martin.apkanalyzer.core.apps.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.apps.permissions.toPermissionDetails
import sk.styk.martin.apkanalyzer.core.apps.sdkversion.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateExtractor
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.AppReferenceCacheKey
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import sk.styk.martin.apkanalyzer.core.common.model.toCacheKey
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTrace
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.analysisMode
import sk.styk.martin.apkanalyzer.core.common.performance.analysisModeAttribute
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import sk.styk.martin.apkanalyzer.core.common.performance.timedSection
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppDetailRepositoryImpl"

@Suppress("TooManyFunctions")
@Singleton
internal class AppDetailRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val sdkVersionResolver: SdkVersionResolver,
    private val installSourceResolver: InstallSourceResolver,
    private val certificateExtractor: CertificateExtractor,
    private val permissionDefinitionResolver: PermissionDefinitionResolver,
    private val storageStatsRepository: StorageStatsRepository,
    private val usageStatsRepository: UsageStatsRepository,
    packageChangesObserver: PackageChangesObserver,
    appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val performanceTracker: PerformanceTracker,
) : AppDetailRepository {

    private val cache = ConcurrentHashMap<AppReferenceCacheKey, AppDetail>()

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

    override suspend fun details(reference: AppReference): Result<AppDetail> = performanceTracker.startCancellableTrace("app_detail_load") {
        analysisMode = reference.analysisModeAttribute
        when (reference) {
            is AppReference.InstalledPackage -> installedPackageDetails(reference.packageName)
            is AppReference.ApkFile -> apkFilePackageDetails(File(reference.path))
        }.onSuccess {
            outcome = TraceOutcome.Success
        }.onFailure {
            recordErrorPreserving(it)
        }
    }

    private suspend fun PerformanceTrace.installedPackageDetails(packageName: PackageName): Result<AppDetail> {
        val context = "mode=installed package=${packageName.value}"
        val reference = AppReference.InstalledPackage(packageName)
        val cacheKey = reference.toCacheKey()
        Logger.i(TAG, "App detail loading started: $context")
        val cachedDetail = cache[cacheKey]
        cachedDetail?.let {
            markCacheHit()
            Logger.i(TAG, "App detail loading finished: cache hit, $context")
            return Result.success(it)
        }
        return runCatchingCancellable {
            val packageInfo = timedStage("package query", "package_query_ms", context) {
                packageManager.getPackageInfo(packageName.value, analysisFlags)
            }
            val totalSize = timedStage("storage stats", "storage_stats_ms", context) {
                storageStatsRepository.queryTotalSize(packageName)
            }.warnIfDegraded("storage stats", context)
            val lastUsedTime = timedStage("usage stats", "usage_stats_ms", context) {
                usageStatsRepository.queryLastUsedTime(packageName)
            }.warnIfDegraded("usage stats", context)

            getPackageDetails(
                context = context,
                analysisMode = AppDetail.AnalysisMode.InstalledPackage,
                packageInfo = packageInfo,
                totalSize = totalSize,
                lastUsedTime = lastUsedTime,
            )
        }.onSuccess { detail ->
            cache[cacheKey] = detail
            Logger.i(TAG, "App detail loading finished: $context")
        }.onFailure {
            Logger.e(TAG, it, "App detail loading failed: $context")
        }
    }

    private suspend fun PerformanceTrace.apkFilePackageDetails(accessibleFile: File): Result<AppDetail> {
        val context = "mode=apk_file apk_path=${accessibleFile.absolutePath}"
        val reference = AppReference.ApkFile(accessibleFile.absolutePath)
        val cacheKey = reference.toCacheKey()
        Logger.i(TAG, "App detail loading started: $context")
        val cachedDetail = cache[cacheKey]
        cachedDetail?.let {
            markCacheHit()
            Logger.i(TAG, "App detail loading finished: cache hit, $context")
            return Result.success(it)
        }

        return runCatchingCancellable {
            val packageInfo = timedStage("package query", "package_query_ms", context) {
                packageManager.getPackageArchiveInfoWithCorrectPath(accessibleFile.absolutePath, analysisFlags)
            } ?: error("Cannot parse APK file: ${accessibleFile.absolutePath}")

            getPackageDetails(
                context = context,
                analysisMode = AppDetail.AnalysisMode.ApkFile,
                packageInfo = packageInfo,
            )
        }.onSuccess { detail ->
            cache[cacheKey] = detail
            Logger.i(TAG, "App detail loading finished: $context")
        }.onFailure {
            Logger.e(TAG, it, "App detail loading failed: $context")
        }
    }

    private fun PerformanceTrace.getPackageDetails(
        context: String,
        analysisMode: AppDetail.AnalysisMode,
        packageInfo: PackageInfo,
        totalSize: AppSize? = null,
        lastUsedTime: Instant? = null,
    ): AppDetail {
        val info = timedStage("general information", "general_info_ms", context) {
            getGeneralData(packageInfo, totalSize, lastUsedTime)
        }

        val signing = timedStage("certificates", "certificates_ms", context) {
            certificateExtractor.getAppSigning(packageInfo)
        }

        val launcherActivityNames = when (analysisMode) {
            AppDetail.AnalysisMode.InstalledPackage -> timedStage("launcher activities", "launcher_activities_ms", context) {
                queryLauncherActivityNames(PackageName(packageInfo.packageName))
            }

            AppDetail.AnalysisMode.ApkFile -> null
        }

        val components = timedStage("components", "components_ms", context) {
            ComponentsBundle(
                activities = getActivities(packageInfo, launcherActivityNames),
                services = getServices(packageInfo),
                contentProviders = getContentProviders(packageInfo),
                receivers = getBroadcastReceivers(packageInfo),
            )
        }

        val permissions = timedStage("permissions", "permissions_ms", context) {
            getPermissions(packageInfo)
        }

        val features = timedStage("features", "features_ms", context) {
            getFeatures(packageInfo)
        }

        return AppDetail(
            analysisMode = analysisMode,
            info = info,
            signing = signing,
            activities = components.activities,
            services = components.services,
            contentProviders = components.contentProviders,
            receivers = components.receivers,
            permissions = permissions,
            features = features,
        )
    }

    private inline fun <T> PerformanceTrace.timedStage(
        stage: String,
        metric: String,
        context: String,
        block: () -> T,
    ): T = timedSection(tag = TAG, operation = "App detail $stage loading", metric = metric, context = context, block = block)

    private fun PerformanceTrace.markCacheHit() {
        this[CACHE_HIT_ATTRIBUTE] = true.toString()
    }

    private fun warnDegraded(stage: String, context: String) {
        Logger.w(TAG, "App detail $stage loading degraded: data unavailable, $context")
    }

    private fun <T> T?.warnIfDegraded(stage: String, context: String): T? {
        if (this == null) warnDegraded(stage, context)
        return this
    }

    private data class ComponentsBundle(
        val activities: List<Activity>,
        val services: List<Service>,
        val contentProviders: List<ContentProvider>,
        val receivers: List<BroadcastReceiver>,
    )

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

    private fun getActivities(packageInfo: PackageInfo, launcherActivityNames: Set<String>?): List<Activity> = packageInfo.activities.orEmpty().map {
        Activity(
            name = it.name,
            packageName = PackageName(it.packageName),
            label = it.loadLabel(packageManager).toString(),
            targetActivity = it.targetActivity,
            permission = it.permission,
            parentName = it.parentActivityName,
            isExported = it.exported,
            isLauncher = launcherActivityNames?.contains(it.name),
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
            pathPermissions = resolvePathPermissions(it.pathPermissions),
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
            details = it.toPermissionDetails(packageManager),
        )
    }

    private fun getUsedPermissions(packageInfo: PackageInfo): List<UsedPermission> {
        val grantedFlags = packageInfo.requestedPermissionsFlags
        return packageInfo.requestedPermissions.orEmpty().mapIndexed { index, name ->
            UsedPermission(
                permissionData = Permission(
                    name = name,
                    details = permissionDefinitionResolver.resolve(name),
                ),
                isGranted = (grantedFlags?.getOrNull(index) ?: 0) and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0,
            )
        }
    }

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
}

private val launcherCategories = listOf(Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER)

private fun PerformanceTrace.recordErrorPreserving(failure: Throwable) {
    val telemetryFailure = runCatching {
        outcome = TraceOutcome.Error
    }.exceptionOrNull()
    if (telemetryFailure != null && telemetryFailure !== failure) {
        failure.addSuppressed(telemetryFailure)
    }
}

private const val CACHE_HIT_ATTRIBUTE = "cache_hit"

private val SPLIT_ABI_QUALIFIERS = mapOf(
    "armeabi" to "armeabi",
    "armeabi_v7a" to "armeabi-v7a",
    "arm64_v8a" to "arm64-v8a",
    "x86" to "x86",
    "x86_64" to "x86_64",
    "mips" to "mips",
    "mips64" to "mips64",
)

private val SPLIT_DENSITY_QUALIFIERS = setOf(
    "ldpi", "mdpi", "tvdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "nodpi", "anydpi",
)

private fun computeApkSize(applicationInfo: ApplicationInfo?): AppSize {
    val baseApkSize = applicationInfo?.sourceDir?.let { File(it).length() } ?: 0L
    val splitApksSize = readInstalledSplits(applicationInfo).sumOf { it.size.bytes }
    return (baseApkSize + splitApksSize).bytes
}

private fun readInstalledSplits(applicationInfo: ApplicationInfo?): List<InstalledSplitApk> = applicationInfo?.splitSourceDirs.orEmpty().map { path ->
    val file = File(path)
    val (kind, qualifier) = classifySplitApk(file.name)
    InstalledSplitApk(
        fileName = file.name,
        filePath = path,
        size = file.length().bytes,
        kind = kind,
        qualifier = qualifier,
    )
}

private fun classifySplitApk(fileName: String): Pair<SplitApkKind, String> {
    val baseName = fileName.removeSuffix(".apk")
    val configQualifier = baseName.removePrefix("split_config.")
    if (configQualifier != baseName) {
        val abi = SPLIT_ABI_QUALIFIERS[configQualifier.lowercase()]
        return when {
            abi != null -> SplitApkKind.Abi to abi
            configQualifier.lowercase() in SPLIT_DENSITY_QUALIFIERS -> SplitApkKind.ScreenDensity to configQualifier
            else -> SplitApkKind.Language to configQualifier
        }
    }
    val moduleName = baseName.removePrefix("split_").substringBefore(".config.")
    return SplitApkKind.DynamicFeature to moduleName
}
