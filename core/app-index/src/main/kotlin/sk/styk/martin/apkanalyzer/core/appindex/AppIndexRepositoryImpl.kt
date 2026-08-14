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
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigningRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.appCount
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import sk.styk.martin.apkanalyzer.core.common.performance.timedSection
import javax.inject.Inject

private const val TAG = "AppIndexRepositoryImpl"

internal class AppIndexRepositoryImpl @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    appSigningRepository: AppSigningRepository,
    dispatcherProvider: DispatcherProvider,
    appScope: CoroutineScope,
    private val performanceTracker: PerformanceTracker,
) : AppIndexRepository {

    private val cachedIndex = combine(
        installedAppsRepository.apps(),
        appSigningRepository.signing(),
    ) { apps, signing -> AppIndexStatus.Data(buildIndex(apps, signing)) as AppIndexStatus }
        .onStart { emit(AppIndexStatus.Loading) }
        .flowOn(dispatcherProvider.default())
        .shareIn(appScope, SharingStarted.Lazily, replay = 1)

    override fun index(): Flow<AppIndexStatus> = cachedIndex

    private suspend fun buildIndex(
        apps: List<InstalledApp>,
        signing: Map<PackageName, AppSigning>,
    ) = performanceTracker.startCancellableTrace("app_index_build") {
        appCount = apps.size
        runCatchingCancellable {
            timedSection(tag = TAG, operation = "App index build", metric = "index_build_ms") {
                AppAttributeIndex(
                    targetSdk = apps.byTargetSdk(),
                    minSdk = apps.byMinSdk(),
                    installSource = apps.byInstallSource(),
                    permission = apps.byPermission(),
                    certificateSha256 = apps.byCertificate(signing) { it.certificateHashSha256 },
                    certificateSha1 = apps.byCertificate(signing) { it.certificateHashSha1 },
                    certificateMd5 = apps.byCertificate(signing) { it.certificateHashMd5 },
                    certificateOrganization = apps.byCertificate(signing) { it.subject.organization },
                    certificateCountry = apps.byCertificate(signing) { it.subject.country },
                    certificateByHash = apps.certificatesByHash(signing),
                    sharedUserId = apps.bySharedUserId(),
                    appCategory = apps.byAppCategory(),
                )
            }
        }.fold(
            onSuccess = { index ->
                outcome = TraceOutcome.Success
                index
            },
            onFailure = { failure ->
                outcome = TraceOutcome.Error
                Logger.e(TAG, failure, "App index build failed")
                AppAttributeIndex(
                    targetSdk = emptyMap(),
                    minSdk = emptyMap(),
                    installSource = emptyMap(),
                    permission = emptyMap(),
                    certificateSha256 = emptyMap(),
                    certificateSha1 = emptyMap(),
                    certificateMd5 = emptyMap(),
                    certificateOrganization = emptyMap(),
                    certificateCountry = emptyMap(),
                    certificateByHash = emptyMap(),
                    sharedUserId = emptyMap(),
                    appCategory = emptyMap(),
                )
            },
        )
    }

    private fun List<InstalledApp>.byTargetSdk() = groupBy(InstalledApp::targetSdk, InstalledApp::packageName)

    private fun List<InstalledApp>.byMinSdk() = groupBy(InstalledApp::minSdk, InstalledApp::packageName)

    private fun List<InstalledApp>.byInstallSource() = groupBy(InstalledApp::source, InstalledApp::packageName)

    private fun List<InstalledApp>.byPermission() = flatMap { app -> app.requestedPermissions.map { it to app.packageName } }
        .groupBy({ it.first }, { it.second })

    private fun List<InstalledApp>.bySharedUserId() = mapNotNull { app -> app.sharedUserId?.let { it to app.packageName } }
        .groupBy({ it.first }, { it.second })

    private fun List<InstalledApp>.byAppCategory() = groupBy(InstalledApp::category, InstalledApp::packageName)

    private fun <T> List<InstalledApp>.byCertificate(
        signing: Map<PackageName, AppSigning>,
        key: (Certificate) -> T,
    ): Map<T, List<PackageName>> = flatMap { app -> signing[app.packageName]?.currentCertificates.orEmpty().map { key(it) to app.packageName } }
        .groupBy({ it.first }, { it.second })

    private fun List<InstalledApp>.certificatesByHash(signing: Map<PackageName, AppSigning>): Map<String, Certificate> {
        val certificates = flatMap { app -> signing[app.packageName]?.currentCertificates.orEmpty() }
        return buildMap {
            certificates.forEach { certificate ->
                putIfAbsent(certificate.certificateHashSha256, certificate)
                putIfAbsent(certificate.certificateHashSha1, certificate)
                putIfAbsent(certificate.certificateHashMd5, certificate)
            }
        }
    }
}
