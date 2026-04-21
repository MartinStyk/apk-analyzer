package sk.styk.martin.apkanalyzer.core.applist

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import sk.styk.martin.apkanalyzer.core.appanalysis.AppInstallSourceManager
import sk.styk.martin.apkanalyzer.core.applist.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import java.io.File
import javax.inject.Inject

class InstalledAppsRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val appInstallSourceManager: AppInstallSourceManager,
    private val packageChangesObserver: PackageChangesObserver,
    private val dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
) : InstalledAppsRepository {

    private val cachedApps = packageChangesObserver.observe()
        .onStart { emit(Unit) }
        .map { loadAllApps() }
        .flowOn(dispatcherProvider.io())
        .shareIn(appScope, SharingStarted.Eagerly, replay = 1)

    override fun apps(): Flow<List<InstalledApp>> = cachedApps

    override fun apps(packageNames: List<String>): Flow<List<InstalledApp>> = packageChangesObserver.observe()
        .onStart { emit(Unit) }
        .map { loadApps(packageNames) }
        .flowOn(dispatcherProvider.io())

    private fun loadAllApps(): List<InstalledApp> = packageManager.getInstalledPackages(0).mapNotNull { packageInfo ->
        packageInfo.applicationInfo?.let { packageInfo.toInstalledApp() }
    }

    private fun loadApps(packageNames: List<String>): List<InstalledApp> = packageNames.mapNotNull { name ->
        try {
            val packageInfo = packageManager.getPackageInfo(name, 0)
            packageInfo?.applicationInfo?.let { packageInfo.toInstalledApp() }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun PackageInfo.toInstalledApp(): InstalledApp {
        val appInfo = applicationInfo
        return InstalledApp(
            packageName = packageName,
            applicationName = appInfo?.loadLabel(packageManager)?.toString() ?: packageName,
            isSystemApp = appInstallSourceManager.isSystemInstalledApp(this),
            version = longVersionCode,
            source = appInstallSourceManager.getAppInstallSource(this),
            targetSdk = appInfo?.targetSdkVersion ?: 0,
            apkSize = AppSize(appInfo?.sourceDir?.let { File(it).length() } ?: 0L),
            versionName = versionName,
            installTime = firstInstallTime,
        )
    }
}
