package sk.styk.martin.apkanalyzer.core.apphistory.capture

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryCaptureKey
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryPersistenceRepository
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryRecord
import sk.styk.martin.apkanalyzer.core.apphistory.storage.CapturedSection
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.intentfilters.IntentFiltersRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibrariesRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.device.DeviceIdProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.appCount
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import java.time.Instant
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
    private val persistenceRepository: AppHistoryPersistenceRepository,
    private val deviceIdProvider: DeviceIdProvider,
    private val dispatcherProvider: DispatcherProvider,
    private val performanceTracker: PerformanceTracker,
) : AppHistoryCaptureRepository {

    private val perPackageMutexes = ConcurrentHashMap<PackageName, Mutex>()

    private val InstalledApp.captureKey: AppHistoryCaptureKey
        get() = AppHistoryCaptureKey(deviceId = deviceIdProvider.deviceId, firstInstallTime = installTime, lastUpdateTime = lastUpdateTime)

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
            val latestByPackage = persistenceRepository.latestCaptureKeys().getOrThrow()
            var capturedCount = 0
            var failedCount = 0
            apps.forEach { app ->
                if (app.hasChangedSince(latestByPackage[app.packageName])) {
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
        if (!app.hasChangedSince(persistenceRepository.latestCaptureKey(app.packageName).getOrThrow())) {
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

        val detail = detailDeferred.await().getOrElse {
            Logger.w(APP_HISTORY, it, "App detail load failed for ${app.packageName.value}, skipping capture")
            return@coroutineScope CaptureOutcome.Aborted(it)
        }

        val record = AppHistoryRecord(
            packageName = app.packageName,
            capturedAt = Instant.now(),
            captureKey = app.captureKey,
            detail = detail,
            intentFilters = intentFiltersDeferred.await().toCapturedSection(app.packageName, "Intent filters"),
            nativeLibraries = nativeLibrariesDeferred.await().toCapturedSection(app.packageName, "Native libraries"),
            signingScheme = signingSchemeDeferred.await().toCapturedSection(app.packageName, "Signing scheme"),
        )
        persistenceRepository.save(record).fold(
            onSuccess = {
                CaptureOutcome.Completed(
                    degradedSectionCount = listOf(
                        record.intentFilters,
                        record.nativeLibraries,
                        record.signingScheme
                    ).count { it is CapturedSection.Unavailable },
                )
            },
            onFailure = {
                Logger.w(APP_HISTORY, it, "App history save failed for ${app.packageName.value}")
                return@coroutineScope CaptureOutcome.Aborted(it)
            },
        )
    }

    private fun <T> Result<T>.toCapturedSection(packageName: PackageName, sectionName: String): CapturedSection<T> = fold(
        onSuccess = { CapturedSection.Captured(it) },
        onFailure = {
            Logger.w(APP_HISTORY, it, "$sectionName extraction failed for ${packageName.value}")
            CapturedSection.Unavailable
        },
    )

    private fun InstalledApp.hasChangedSince(latest: AppHistoryCaptureKey?): Boolean = latest != captureKey

    private sealed interface CaptureOutcome {
        data class Aborted(val cause: Throwable) : CaptureOutcome
        data class Completed(val degradedSectionCount: Int) : CaptureOutcome
    }
}
