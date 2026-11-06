package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.appfunctions.model.CertificateSummary
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepositoryFake
import sk.styk.martin.apkanalyzer.core.apps.model.WhatsApp
import sk.styk.martin.apkanalyzer.core.apps.model.packageName
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.signing.GoogleCertificate
import sk.styk.martin.apkanalyzer.core.apps.signing.WhatsAppCertificate
import sk.styk.martin.apkanalyzer.core.common.coroutines.TestDispatcherProvider
import java.time.Instant

class GetAppCertificatesUseCaseTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    private val appDetailRepository = AppDetailRepositoryFake(WhatsApp)
    private val useCase = GetAppCertificatesUseCase(appDetailRepository, dispatcherProvider)

    @Test
    fun `Given a current certificate When getting certificates Then summary contains its details`() = runTest {
        val certificate = WhatsAppCertificate.copy(
            signAlgorithm = "SHA256withECDSA",
            certificateHashSha256 = "a1b2c3",
            validFrom = Instant.parse("2024-02-03T00:00:00Z"),
            validUntil = Instant.parse("2034-02-03T00:00:00Z"),
            issuer = CertificatePrincipal(organization = "Acme"),
            isSelfSigned = false,
        )
        appDetailRepository += WhatsApp.copy(signing = AppSigning(currentCertificates = listOf(certificate)))

        val certificates = useCase(WhatsApp.packageName)

        certificates shouldBe listOf(
            CertificateSummary(
                signatureAlgorithm = "SHA256withECDSA",
                isSelfSigned = false,
                sha256Fingerprint = "A1:B2:C3",
                validFrom = Instant.parse("2024-02-03T00:00:00Z"),
                validUntil = Instant.parse("2034-02-03T00:00:00Z"),
                issuerOrganization = "Acme",
            ),
        )
    }

    @Test
    fun `Given multiple current certificates When getting certificates Then all are returned in order`() = runTest {
        val first = WhatsAppCertificate.copy(certificateHashSha256 = "aa")
        val second = GoogleCertificate.copy(certificateHashSha256 = "bb")
        appDetailRepository += WhatsApp.copy(signing = AppSigning(currentCertificates = listOf(first, second)))

        val certificates = useCase(WhatsApp.packageName)

        certificates.map { it.sha256Fingerprint } shouldBe listOf("AA", "BB")
    }

    @Test
    fun `Given only past certificates When getting certificates Then result is empty`() = runTest {
        appDetailRepository += WhatsApp.copy(signing = AppSigning(pastCertificates = listOf(WhatsAppCertificate)))

        val certificates = useCase(WhatsApp.packageName)

        certificates.shouldBeEmpty()
    }
}
