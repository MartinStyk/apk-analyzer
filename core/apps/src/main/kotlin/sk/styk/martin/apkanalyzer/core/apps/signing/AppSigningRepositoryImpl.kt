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
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "AppSigningRepositoryImpl"

internal class AppSigningRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val certificateExtractor: CertificateExtractor,
    packageChangesObserver: PackageChangesObserver,
    dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
) : AppSigningRepository {

    @Suppress("TooGenericExceptionCaught")
    private val cachedSigning = packageChangesObserver.observe()
        .onStart { emit(Unit) }
        .mapLatest {
            Logger.d(TAG, "App signing index loading started")
            try {
                val result = loadAllSigning()
                Logger.i(TAG, "App signing index loading finished: ${result.size} apps loaded")
                result
            } catch (failure: Throwable) {
                if (failure !is CancellationException) {
                    Logger.e(TAG, failure, "App signing index loading failed")
                }
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
