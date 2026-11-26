package sk.styk.martin.apkanalyzer.core.apps.signing

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import sk.styk.martin.apkanalyzer.core.common.performance.timedSection
import javax.inject.Inject

private const val TAG = "AppSigningRepositoryImpl"

internal class AppSigningRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val certificateExtractor: CertificateExtractor,
    packageChangesObserver: PackageChangesObserver,
    dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
    private val performanceTracker: PerformanceTracker,
) : AppSigningRepository {

    private val cachedSigning = packageChangesObserver.observe()
        .map { Unit }
        .onStart { emit(Unit) }
        .mapLatest { loadAllSigning() }
        .flowOn(dispatcherProvider.io())
        .shareIn(appScope, SharingStarted.Lazily, replay = 1)

    override fun signing(): Flow<Map<PackageName, AppSigning>> = cachedSigning

    @SuppressLint("QueryPermissionsNeeded")
    private suspend fun loadAllSigning(): Map<PackageName, AppSigning> = performanceTracker.startCancellableTrace("app_signing_index_load") {
        runCatchingCancellable {
            val packages = timedSection(tag = TAG, operation = "App signing package query", metric = "package_query_ms") {
                packageManager.getInstalledPackages(PackageManager.GET_SIGNING_CERTIFICATES)
            }
            timedSection(tag = TAG, operation = "App signing extraction", metric = "signing_extraction_ms") {
                packages.associate { PackageName(it.packageName) to certificateExtractor.getAppSigning(it) }
            }
        }.fold(
            onSuccess = { signing ->
                outcome = TraceOutcome.Success
                Logger.i(TAG, "App signing index loading finished: ${signing.size} apps loaded")
                signing
            },
            onFailure = { failure ->
                outcome = TraceOutcome.Error
                Logger.e(TAG, failure, "App signing index loading failed")
                emptyMap()
            },
        )
    }
}
