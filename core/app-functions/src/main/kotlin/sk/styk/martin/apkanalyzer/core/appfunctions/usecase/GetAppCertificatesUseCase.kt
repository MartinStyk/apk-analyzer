package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.appfunctions.model.CertificateSummary
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

internal class GetAppCertificatesUseCase @Inject constructor(
    private val appDetailRepository: AppDetailRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(packageName: String): List<CertificateSummary> = withContext(dispatcherProvider.io()) {
        appDetailRepository.details(AppReference.InstalledPackage(PackageName(packageName)))
            .getOrElse { throw notInstalled(packageName) }
            .signing.currentCertificates.map { it.toCertificateSummary() }
    }
}

private fun Certificate.toCertificateSummary() = CertificateSummary(
    signatureAlgorithm = signAlgorithm,
    isSelfSigned = isSelfSigned,
    sha256Fingerprint = formattedSha256Fingerprint,
    validFrom = validFrom,
    validUntil = validUntil,
    issuerOrganization = issuer.organization,
)
