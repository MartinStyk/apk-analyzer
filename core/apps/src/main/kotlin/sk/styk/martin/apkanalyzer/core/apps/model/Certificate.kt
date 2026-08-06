package sk.styk.martin.apkanalyzer.core.apps.model

import java.time.Instant

data class Certificate(
    val signAlgorithm: String,
    val certificateHashMd5: String,
    val certificateHashSha1: String,
    val certificateHashSha256: String,
    val publicKeyMd5: String,
    val publicKeySha1: String,
    val publicKeySha256: String,
    val validFrom: Instant,
    val validUntil: Instant,
    val serialNumber: String,
    val issuer: CertificatePrincipal = CertificatePrincipal(),
    val subject: CertificatePrincipal = CertificatePrincipal(),
    val trustLevel: CertificateTrustLevel = CertificateTrustLevel.Valid,
    val isSelfSigned: Boolean,
) {
    val formattedSha256Fingerprint: String
        get() = certificateHashSha256.uppercase().chunked(2).joinToString(":")
}
