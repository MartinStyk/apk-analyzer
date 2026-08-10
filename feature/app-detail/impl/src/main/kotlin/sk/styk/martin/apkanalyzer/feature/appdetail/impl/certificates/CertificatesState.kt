package sk.styk.martin.apkanalyzer.feature.appdetail.impl.certificates

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import sk.styk.martin.apkanalyzer.core.apps.model.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.apps.model.SignatureAlgorithmAssessment
import sk.styk.martin.apkanalyzer.core.apps.model.SigningSchemeVersion
import java.time.Instant

internal enum class CertificateValidity {
    Valid,
    Expired,
    NotYetValid,
}

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

    data object Error : CertificatesState

    @Immutable
    data class Loaded(
        val currentCertificates: ImmutableList<CertificateItem>,
        val pastCertificates: ImmutableList<CertificateItem>,
        val hasMultipleSigners: Boolean,
        val signingSchemeVersions: ImmutableList<SigningSchemeVersion>?,
    ) : CertificatesState
}
