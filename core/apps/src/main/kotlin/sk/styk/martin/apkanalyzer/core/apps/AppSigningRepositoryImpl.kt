package sk.styk.martin.apkanalyzer.core.apps

import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import sk.styk.martin.apkanalyzer.core.apps.analysis.CertificateExtractor
import sk.styk.martin.apkanalyzer.core.apps.model.AppSigning
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import javax.inject.Inject

internal class AppSigningRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val certificateExtractor: CertificateExtractor,
    private val packageChangesObserver: PackageChangesObserver,
    private val dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
) : AppSigningRepository {

    private val cachedSigning = packageChangesObserver.observe()
        .onStart { emit(Unit) }
        .mapLatest { loadAllSigning() }
        .flowOn(dispatcherProvider.io())
        .shareIn(appScope, SharingStarted.Lazily, replay = 1)

    override fun signing(): Flow<Map<String, AppSigning>> = cachedSigning

    private fun loadAllSigning(): Map<String, AppSigning> = packageManager.getInstalledPackages(PackageManager.GET_SIGNING_CERTIFICATES)
        .associate { it.packageName to certificateExtractor.getAppSigning(it) }
}
