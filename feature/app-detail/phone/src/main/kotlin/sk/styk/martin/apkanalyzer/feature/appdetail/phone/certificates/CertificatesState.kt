package sk.styk.martin.apkanalyzer.feature.appdetail.phone.certificates

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateValidity
import sk.styk.martin.apkanalyzer.core.apps.signing.SignatureAlgorithmAssessment
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.SectionErrorReason
import java.time.Instant

@Immutable
internal data class CertificateItem(
    val signAlgorithm: String,
    val signatureAlgorithmAssessment: SignatureAlgorithmAssessment,
    val certificateHashMd5: String,
    val certificateHashSha1: String,
    val certificateHashSha256: String,
    val publicKeyMd5: String,
    val publicKeySha1: String,
    val publicKeySha256: String,
    val validFrom: Instant,
    val validUntil: Instant,
    val serialNumber: String,
    val issuer: CertificatePrincipal,
    val subject: CertificatePrincipal,
    val trustLevel: CertificateTrustLevel,
    val isSelfSigned: Boolean,
    val validity: CertificateValidity,
)

@Immutable
internal sealed interface CertificatesState {
    data object Loading : CertificatesState

    data class Error(val reason: SectionErrorReason) : CertificatesState

    @Immutable
    data class Loaded(
        val currentCertificates: List<CertificateItem>,
        val pastCertificates: List<CertificateItem>,
        val hasMultipleSigners: Boolean,
        val signingSchemeVersions: List<SigningSchemeVersion>?,
        val isHistorical: Boolean = false,
    ) : CertificatesState
}
