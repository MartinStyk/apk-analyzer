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
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class AppSigningRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val certificateExtractor: CertificateExtractor,
    packageChangesObserver: PackageChangesObserver,
    dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
) : AppSigningRepository {

    private val cachedSigning = packageChangesObserver.observe()
        .onStart { emit(Unit) }
        .mapLatest { loadAllSigning() }
        .flowOn(dispatcherProvider.io())
        .shareIn(appScope, SharingStarted.Lazily, replay = 1)

    override fun signing(): Flow<Map<PackageName, AppSigning>> = cachedSigning

    private fun loadAllSigning(): Map<PackageName, AppSigning> = packageManager.getInstalledPackages(PackageManager.GET_SIGNING_CERTIFICATES)
        .associate { PackageName(it.packageName) to certificateExtractor.getAppSigning(it) }
}
