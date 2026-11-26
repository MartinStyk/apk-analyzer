package sk.styk.martin.apkanalyzer.core.appindex.api.browse

import sk.styk.martin.apkanalyzer.core.apps.api.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.api.signing.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.api.signing.CertificateValidity
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SignatureAlgorithmAssessment
import java.time.Instant

sealed interface BrowseBucketDetail {

    data class Permission(val protectionLevel: ProtectionLevel?, val description: String?) : BrowseBucketDetail

    data class Certificate(
        val algorithm: String,
        val algorithmAssessment: SignatureAlgorithmAssessment,
        val isSelfSigned: Boolean,
        val subject: CertificatePrincipal,
        val issuer: CertificatePrincipal,
        val validFrom: Instant,
        val validUntil: Instant,
        val validity: CertificateValidity,
    ) : BrowseBucketDetail
}
