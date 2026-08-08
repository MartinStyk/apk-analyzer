package sk.styk.martin.apkanalyzer.core.appindex

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import sk.styk.martin.apkanalyzer.core.appindex.model.AppAttributeIndex
import sk.styk.martin.apkanalyzer.core.appindex.model.AppIndexStatus
import sk.styk.martin.apkanalyzer.core.apps.AppSigningRepository
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.model.Certificate
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class AppIndexRepositoryImpl @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    appSigningRepository: AppSigningRepository,
    dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
) : AppIndexRepository {

    private val cachedIndex = combine(
        installedAppsRepository.apps(),
        appSigningRepository.signing(),
    ) { apps, signing -> AppIndexStatus.Data(buildIndex(apps, signing)) as AppIndexStatus }
        .onStart { emit(AppIndexStatus.Loading) }
        .flowOn(dispatcherProvider.default())
        .shareIn(appScope, SharingStarted.Lazily, replay = 1)

    override fun index(): Flow<AppIndexStatus> = cachedIndex

    private fun buildIndex(apps: List<InstalledApp>, signing: Map<PackageName, AppSigning>) = AppAttributeIndex(
        targetSdk = apps.byTargetSdk(),
        minSdk = apps.byMinSdk(),
        installSource = apps.byInstallSource(),
        permission = apps.byPermission(),
        certificateFingerprint = apps.byCertificate(signing) { it.certificateHashSha256 },
        certificateOrganization = apps.byCertificate(signing) { it.subject.organization },
        certificateCountry = apps.byCertificate(signing) { it.subject.country },
        sharedUserId = apps.bySharedUserId(),
        appCategory = apps.byAppCategory(),
    )

    private fun List<InstalledApp>.byTargetSdk() = groupBy(InstalledApp::targetSdk, InstalledApp::packageName)

    private fun List<InstalledApp>.byMinSdk() = groupBy(InstalledApp::minSdk, InstalledApp::packageName)

    private fun List<InstalledApp>.byInstallSource() = groupBy(InstalledApp::source, InstalledApp::packageName)

    private fun List<InstalledApp>.byPermission() = flatMap { app -> app.requestedPermissions.map { it to app.packageName } }
        .groupBy({ it.first }, { it.second })

    private fun List<InstalledApp>.bySharedUserId() = mapNotNull { app -> app.sharedUserId?.let { it to app.packageName } }
        .groupBy({ it.first }, { it.second })

    private fun List<InstalledApp>.byAppCategory() = groupBy(InstalledApp::category, InstalledApp::packageName)

    private fun <T> List<InstalledApp>.byCertificate(signing: Map<PackageName, AppSigning>, key: (Certificate) -> T): Map<T, List<PackageName>> =
        flatMap { app -> signing[app.packageName]?.currentCertificates.orEmpty().map { key(it) to app.packageName } }
            .groupBy({ it.first }, { it.second })
}
