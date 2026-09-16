package sk.styk.martin.apkanalyzer.core.apphistory.capture

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot.ComponentIntentFilterEntrySnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot.toSnapshot
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryGateDao
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryWriteDao
import sk.styk.martin.apkanalyzer.core.apphistory.storage.entity.AppHistoryBlobEntity
import sk.styk.martin.apkanalyzer.core.apphistory.storage.entity.AppHistorySnapshotEntity
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.intentfilters.IntentFiltersRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibrariesRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.device.DeviceIdProvider
import sk.styk.martin.apkanalyzer.core.common.digest.DigestManager
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.appCount
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

internal const val APP_HISTORY = "AppHistory"

@Singleton
internal class AppHistoryCaptureRepositoryImpl @Inject constructor(
    private val installedAppsRepository: InstalledAppsRepository,
    private val appDetailRepository: AppDetailRepository,
    private val intentFiltersRepository: IntentFiltersRepository,
    private val nativeLibrariesRepository: NativeLibrariesRepository,
    private val signingSchemeRepository: SigningSchemeRepository,
    private val appHistoryGateDao: AppHistoryGateDao,
    private val appHistoryWriteDao: AppHistoryWriteDao,
    private val digestManager: DigestManager,
    private val deviceIdProvider: DeviceIdProvider,
    private val dispatcherProvider: DispatcherProvider,
    private val performanceTracker: PerformanceTracker,
) : AppHistoryCaptureRepository {

    private val perPackageMutexes = ConcurrentHashMap<PackageName, Mutex>()

    override suspend fun reconcile(packageName: PackageName): Result<Unit> = runCatchingCancellable {
        Logger.i(APP_HISTORY, "Package reconcile loading started for ${packageName.value}")
        val app = installedAppsRepository.app(packageName)
        if (app == null) {
            Logger.i(APP_HISTORY, "Package reconcile loading finished for ${packageName.value}: package not found, skipped")
            return@runCatchingCancellable
        }
        val captured = captureIfGateOpen(app)
        val status = if (captured) "captured" else "skipped, no change detected"
        Logger.i(APP_HISTORY, "Package reconcile loading finished for ${packageName.value}: $status")
    }

    override suspend fun reconcileAll(): Result<Unit> = runCatchingCancellable {
        performanceTracker.startCancellableTrace("app_history_reconcile") {
            Logger.i(APP_HISTORY, "Reconciliation sweep loading started")
            val apps = installedAppsRepository.apps().first()
            appCount = apps.size
            val latestByPackage = appHistoryGateDao.latestGateTimestampsForAllPackages().associateBy { it.packageName }
            var capturedCount = 0
            var failedCount = 0
            apps.forEach { app ->
                val gate = latestByPackage[app.packageName.value]
                if (app.hasChangedSince(gate?.lastUpdateTime, gate?.firstInstallTime)) {
                    runCatchingCancellable { captureIfGateOpen(app) }
                        .onSuccess { captured -> if (captured) capturedCount++ }
                        .onFailure {
                            failedCount++
                            Logger.w(APP_HISTORY, it, "Reconciliation capture failed for ${app.packageName.value}, continuing sweep")
                        }
                }
            }
            this["captured_count"] = capturedCount
            this["failed_count"] = failedCount
            outcome = if (failedCount == 0) TraceOutcome.Success else TraceOutcome.Degraded
            Logger.i(APP_HISTORY, "Reconciliation sweep loading finished: ${apps.size} apps scanned, $capturedCount captured")
        }
    }

    private suspend fun captureIfGateOpen(app: InstalledApp): Boolean = perPackageMutexes.computeIfAbsent(app.packageName) { Mutex() }.withLock {
        val gate = appHistoryGateDao.latestGateTimestamps(app.packageName.value)
        if (!app.hasChangedSince(gate?.lastUpdateTime, gate?.firstInstallTime)) {
            return@withLock false
        }
        val result = performanceTracker.startCancellableTrace("app_history_capture") {
            val captureOutcome = capture(app)
            when (captureOutcome) {
                is CaptureOutcome.Aborted -> outcome = TraceOutcome.Error

                is CaptureOutcome.Completed -> {
                    this["degraded_sections"] = captureOutcome.degradedSectionCount
                    outcome = if (captureOutcome.degradedSectionCount == 0) TraceOutcome.Success else TraceOutcome.Degraded
                }
            }
            captureOutcome
        }
        if (result is CaptureOutcome.Aborted) throw result.cause
        true
    }

    private suspend fun capture(app: InstalledApp): CaptureOutcome = coroutineScope {
        val reference = AppReference.InstalledPackage(app.packageName)
        val detailDeferred = async(dispatcherProvider.io()) { appDetailRepository.details(reference) }
        val intentFiltersDeferred = async(dispatcherProvider.io()) { intentFiltersRepository.componentIntentFilters(reference) }
        val nativeLibrariesDeferred = async(dispatcherProvider.io()) { nativeLibrariesRepository.nativeLibraries(reference) }
        val signingSchemeDeferred = async(dispatcherProvider.io()) { signingSchemeRepository.signingSchemeVersions(reference) }

        val detailResult = detailDeferred.await()
        val intentFiltersResult = intentFiltersDeferred.await()
        val nativeLibrariesResult = nativeLibrariesDeferred.await()
        val signingSchemeResult = signingSchemeDeferred.await()

        val detail = detailResult.getOrElse {
            Logger.w(APP_HISTORY, it, "App detail load failed for ${app.packageName.value}, skipping capture")
            return@coroutineScope CaptureOutcome.Aborted(it)
        }

        val sections = hashSections(app.packageName, detail, intentFiltersResult, nativeLibrariesResult, signingSchemeResult)
        appHistoryWriteDao.insertSnapshotWithBlobs(sections.toSnapshotEntity(app, detail, deviceIdProvider.deviceId), sections.blobs)
        CaptureOutcome.Completed(sections.degradedSectionCount)
    }

    private fun hashSections(
        packageName: PackageName,
        detail: AppDetail,
        intentFiltersResult: Result<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>>,
        nativeLibrariesResult: Result<NativeLibraries>,
        signingSchemeResult: Result<List<SigningSchemeVersion>?>,
    ): CapturedSections {
        val packageNameValue = packageName.value
        return CapturedSections(
            permissions = hashSection(packageNameValue, detail.permissions.toSnapshot()),
            activities = hashSection(packageNameValue, detail.activities.map { it.toSnapshot() }.sortedBy { it.name }),
            services = hashSection(packageNameValue, detail.services.map { it.toSnapshot() }.sortedBy { it.name }),
            receivers = hashSection(packageNameValue, detail.receivers.map { it.toSnapshot() }.sortedBy { it.name }),
            providers = hashSection(packageNameValue, detail.contentProviders.map { it.toSnapshot() }.sortedBy { it.name }),
            features = hashSection(packageNameValue, detail.features.map { it.toSnapshot() }.sortedBy { Json.encodeToString(it) }),
            signing = hashSection(packageNameValue, detail.signing.toSnapshot()),
            installedSplits = hashSection(
                packageNameValue,
                detail.info.installedSplits.map { it.toSnapshot() }.sortedBy { it.fileName },
            ),
            intentFilters = intentFiltersResult.fold(
                onSuccess = { filters ->
                    val entries = filters.map { (key, value) ->
                        ComponentIntentFilterEntrySnapshot(key.toSnapshot(), value.map { it.toSnapshot() }.sortedBy { Json.encodeToString(it) })
                    }.sortedWith(compareBy({ it.key.name }, { it.key.kind }))
                    hashSection(packageNameValue, entries)
                },
                onFailure = { logSectionFailure(packageName, "Intent filters", it) },
            ),
            nativeLibraries = nativeLibrariesResult.fold(
                onSuccess = { libraries ->
                    val sorted = libraries.files.map { it.toSnapshot() }.sortedWith(compareBy({ it.name }, { it.abi }, { it.containingApkFileName }))
                    hashSection(packageNameValue, sorted)
                },
                onFailure = { logSectionFailure(packageName, "Native libraries", it) },
            ),
            signingScheme = signingSchemeResult.fold(
                onSuccess = { versions -> hashSection(packageNameValue, versions?.map { it.name }?.sorted()) },
                onFailure = { logSectionFailure(packageName, "Signing scheme", it) },
            ),
        )
    }

    private fun logSectionFailure(
        packageName: PackageName,
        sectionName: String,
        throwable: Throwable,
    ): SectionHash? {
        Logger.w(APP_HISTORY, throwable, "$sectionName extraction failed for ${packageName.value}")
        return null
    }

    private inline fun <reified T> hashSection(packageName: String, content: T): SectionHash {
        val serialized = Json.encodeToString(content)
        val hash = digestManager.sha256Digest(serialized)
        val blob = AppHistoryBlobEntity(packageName = packageName, hash = hash, content = serialized)
        return SectionHash(hash, blob)
    }

    private data class SectionHash(val hash: String, val blob: AppHistoryBlobEntity)

    private data class CapturedSections(
        val permissions: SectionHash,
        val activities: SectionHash,
        val services: SectionHash,
        val receivers: SectionHash,
        val providers: SectionHash,
        val features: SectionHash,
        val signing: SectionHash,
        val installedSplits: SectionHash,
        val intentFilters: SectionHash?,
        val nativeLibraries: SectionHash?,
        val signingScheme: SectionHash?,
    ) {
        val degradedSectionCount: Int
            get() = listOf(intentFilters, nativeLibraries, signingScheme).count { it == null }

        val blobs: List<AppHistoryBlobEntity>
            get() = listOfNotNull(
                permissions.blob, activities.blob, services.blob, receivers.blob, providers.blob,
                features.blob, signing.blob, installedSplits.blob,
                intentFilters?.blob, nativeLibraries?.blob, signingScheme?.blob,
            )
    }

    private fun CapturedSections.toSnapshotEntity(
        app: InstalledApp,
        detail: AppDetail,
        deviceId: String,
    ): AppHistorySnapshotEntity {
        val info = detail.info
        return AppHistorySnapshotEntity(
            packageName = app.packageName.value,
            deviceId = deviceId,
            firstInstallTime = app.installTime.toEpochMilli(),
            lastUpdateTime = app.lastUpdateTime.toEpochMilli(),
            applicationName = info.applicationName,
            processName = info.processName,
            versionCode = info.versionCode,
            versionName = info.versionName,
            isSystemApp = info.isSystemApp,
            isDebuggable = info.isDebuggable,
            allowsBackup = info.allowsBackup,
            usesCleartextTraffic = info.usesCleartextTraffic,
            uid = info.uid,
            sharedUserId = info.sharedUserId,
            description = info.description,
            installLocation = info.installLocation.name,
            installingPackage = info.installSourceChain.installingPackage?.value,
            initiatingPackage = info.installSourceChain.initiatingPackage?.value,
            originatingPackage = info.installSourceChain.originatingPackage?.value,
            apkSize = info.apkSize.bytes,
            targetSdkVersion = info.targetSdkVersion,
            minSdkVersion = info.minSdkVersion,
            permissionsHash = permissions.hash,
            activitiesHash = activities.hash,
            servicesHash = services.hash,
            receiversHash = receivers.hash,
            providersHash = providers.hash,
            featuresHash = features.hash,
            signingHash = signing.hash,
            intentFiltersHash = intentFilters?.hash,
            nativeLibrariesHash = nativeLibraries?.hash,
            signingSchemeHash = signingScheme?.hash,
            installedSplitsHash = installedSplits.hash,
        )
    }

    private sealed interface CaptureOutcome {
        data class Aborted(val cause: Throwable) : CaptureOutcome
        data class Completed(val degradedSectionCount: Int) : CaptureOutcome
    }
}

private fun InstalledApp.hasChangedSince(storedLastUpdateTime: Long?, storedFirstInstallTime: Long?): Boolean =
    storedLastUpdateTime != lastUpdateTime.toEpochMilli() || storedFirstInstallTime != installTime.toEpochMilli()
