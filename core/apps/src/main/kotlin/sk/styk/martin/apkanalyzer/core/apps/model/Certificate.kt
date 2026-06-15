package sk.styk.martin.apkanalyzer.core.apps.model

import java.util.Date

data class Certificate(
    val signAlgorithm: String,
    val certificateHashMd5: String,
    val certificateHashSha1: String,
    val certificateHashSha256: String,
    val publicKeyMd5: String,
    val publicKeySha1: String,
    val publicKeySha256: String,
    val startDate: Date,
    val endDate: Date,
    val serialNumber: Int = 0,
    val issuer: CertificatePrincipal = CertificatePrincipal(),
    val subject: CertificatePrincipal = CertificatePrincipal(),
    val trustLevel: CertificateTrustLevel = CertificateTrustLevel.Valid,
) {
    val formattedSha256Fingerprint: String
        get() = certificateHashSha256.uppercase().chunked(2).joinToString(":")

    val formattedSha1Fingerprint: String
        get() = certificateHashSha1.uppercase().chunked(2).joinToString(":")
}
