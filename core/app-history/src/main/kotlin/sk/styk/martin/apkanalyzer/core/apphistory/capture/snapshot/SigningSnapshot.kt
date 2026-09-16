package sk.styk.martin.apkanalyzer.core.apphistory.capture.snapshot

import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificatePrincipal

@Serializable
internal data class SigningSnapshot(val currentCertificates: List<CertificateSnapshot>, val pastCertificates: List<CertificateSnapshot>)

@Serializable
internal data class CertificateSnapshot(
    val signAlgorithm: String,
    val certificateHashMd5: String,
    val certificateHashSha1: String,
    val certificateHashSha256: String,
    val publicKeyMd5: String,
    val publicKeySha1: String,
    val publicKeySha256: String,
    val validFrom: Long,
    val validUntil: Long,
    val serialNumber: String,
    val issuer: CertificatePrincipalSnapshot,
    val subject: CertificatePrincipalSnapshot,
    val isSelfSigned: Boolean,
)

@Serializable
internal data class CertificatePrincipalSnapshot(
    val name: String?,
    val organization: String?,
    val country: String?,
)

internal fun AppSigning.toSnapshot() = SigningSnapshot(
    currentCertificates = currentCertificates.map { it.toSnapshot() }.sortedBy { it.certificateHashSha256 },
    pastCertificates = pastCertificates.map { it.toSnapshot() }.sortedBy { it.certificateHashSha256 },
)

internal fun Certificate.toSnapshot() = CertificateSnapshot(
    signAlgorithm = signAlgorithm,
    certificateHashMd5 = certificateHashMd5,
    certificateHashSha1 = certificateHashSha1,
    certificateHashSha256 = certificateHashSha256,
    publicKeyMd5 = publicKeyMd5,
    publicKeySha1 = publicKeySha1,
    publicKeySha256 = publicKeySha256,
    validFrom = validFrom.toEpochMilli(),
    validUntil = validUntil.toEpochMilli(),
    serialNumber = serialNumber,
    issuer = issuer.toSnapshot(),
    subject = subject.toSnapshot(),
    isSelfSigned = isSelfSigned,
)

internal fun CertificatePrincipal.toSnapshot() = CertificatePrincipalSnapshot(
    name = name,
    organization = organization,
    country = country,
)
