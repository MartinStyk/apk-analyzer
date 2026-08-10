package sk.styk.martin.apkanalyzer.core.apps.signing

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.logger.nextOperationRequest
import sk.styk.martin.apkanalyzer.core.common.logger.operationLogMessage
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "AppSigningRepositoryImpl"
private const val OPERATION = "device_signing"

internal class AppSigningRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val certificateExtractor: CertificateExtractor,
    packageChangesObserver: PackageChangesObserver,
    dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
) : AppSigningRepository {

    private val cachedSigning = packageChangesObserver.observe()
        .onStart { emit(Unit) }
        .mapLatest {
            val requestId = nextOperationRequest()
            Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "started"))
            try {
                val result = loadAllSigning()
                Logger.i(TAG, operationLogMessage(OPERATION, requestId, event = "succeeded", context = "app_count=${result.size}"))
                result
            } catch (cancellation: CancellationException) {
                Logger.d(TAG, operationLogMessage(OPERATION, requestId, event = "cancelled"))
                throw cancellation
            } catch (failure: Throwable) {
                Logger.e(TAG, failure, operationLogMessage(OPERATION, requestId, event = "failed"))
                throw failure
            }
        }
        .flowOn(dispatcherProvider.io())
        .shareIn(appScope, SharingStarted.Lazily, replay = 1)

    override fun signing(): Flow<Map<PackageName, AppSigning>> = cachedSigning

    @SuppressLint("QueryPermissionsNeeded")
    private fun loadAllSigning(): Map<PackageName, AppSigning> = packageManager.getInstalledPackages(PackageManager.GET_SIGNING_CERTIFICATES)
        .associate { PackageName(it.packageName) to certificateExtractor.getAppSigning(it) }
}
