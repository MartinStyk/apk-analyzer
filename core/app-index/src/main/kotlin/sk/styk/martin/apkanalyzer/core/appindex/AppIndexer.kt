package sk.styk.martin.apkanalyzer.core.appindex

import sk.styk.martin.apkanalyzer.core.appindex.model.AppAttributeIndex
import sk.styk.martin.apkanalyzer.core.apps.model.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.model.Certificate
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp

internal object AppIndexer {

    fun index(apps: List<InstalledApp>, signing: Map<String, AppSigning>): AppAttributeIndex = AppAttributeIndex(
        targetSdk = apps.byTargetSdk(),
        minSdk = apps.byMinSdk(),
        installSource = apps.byInstallSource(),
        permission = apps.byPermission(),
        certificateFingerprint = apps.byCertificate(signing) { it.certificateHashSha256 },
        certificateOrganization = apps.byCertificate(signing) { it.subject.organization },
        certificateCountry = apps.byCertificate(signing) { it.subject.country },
    )

    private fun List<InstalledApp>.byTargetSdk() = groupBy(InstalledApp::targetSdk, InstalledApp::packageName)

    private fun List<InstalledApp>.byMinSdk() = groupBy(InstalledApp::minSdk, InstalledApp::packageName)

    private fun List<InstalledApp>.byInstallSource() = groupBy(InstalledApp::source, InstalledApp::packageName)

    private fun List<InstalledApp>.byPermission() = flatMap { app -> app.requestedPermissions.map { it to app.packageName } }
        .groupBy({ it.first }, { it.second })

    private fun <T> List<InstalledApp>.byCertificate(signing: Map<String, AppSigning>, key: (Certificate) -> T): Map<T, List<String>> =
        flatMap { app -> signing[app.packageName]?.currentCertificates.orEmpty().map { key(it) to app.packageName } }
            .groupBy({ it.first }, { it.second })
}
