package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageInfo
import android.content.pm.Signature
import sk.styk.martin.apkanalyzer.core.apps.model.Certificate
import sk.styk.martin.apkanalyzer.core.apps.model.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.digest.DigestManager
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.security.auth.x500.X500Principal
import javax.security.auth.x500.X500Principal.RFC1779

private const val TAG = "CertificateExtractorImpl"

internal class CertificateExtractorImpl @Inject constructor(private val digestManager: DigestManager) : CertificateExtractor {

    override fun getCertificateData(packageInfo: PackageInfo): List<Certificate> = packageInfo.signingInfo?.apkContentsSigners.orEmpty()
        .mapNotNull { signature ->
            runCatching { signature.toCertificateData() }
                .onFailure { Logger.e(TAG, it, "Could not parse certificate for ${packageInfo.packageName}") }
                .getOrNull()
        }

    private fun Signature.toCertificateData(): Certificate {
        val certificate = ByteArrayInputStream(toByteArray()).use { stream ->
            CertificateFactory.getInstance("X509").generateCertificate(stream) as X509Certificate
        }
        val publicKeyHex = digestManager.byteToHexString(certificate.publicKey.encoded)

        return Certificate(
            signAlgorithm = certificate.sigAlgName,
            certificateHashMd5 = digestManager.md5Digest(certificate.encoded),
            certificateHashSha1 = digestManager.sha1Digest(certificate.encoded),
            certificateHashSha256 = digestManager.sha256Digest(certificate.encoded),
            publicKeyMd5 = digestManager.md5Digest(publicKeyHex),
            publicKeySha1 = digestManager.sha1Digest(publicKeyHex),
            publicKeySha256 = digestManager.sha256Digest(publicKeyHex),
            startDate = certificate.notBefore,
            endDate = certificate.notAfter,
            serialNumber = certificate.serialNumber.toInt(),
            issuer = certificate.issuerX500Principal.toPrincipal(),
            subject = certificate.subjectX500Principal.toPrincipal(),
            trustLevel = resolveTrustLevel(certificate),
        )
    }

    private fun X500Principal.toPrincipal() = CertificatePrincipal(
        name = getField("CN=([^,]*)"),
        organization = getField("O=([^,]*)"),
        country = getField("C=([^,]*)"),
    )

    private fun X500Principal.getField(pattern: String): String? = getName(RFC1779).takeUnless { it.isNullOrBlank() }?.let { Regex(pattern).find(it)?.groupValues?.get(1) }

    private fun resolveTrustLevel(certificate: X509Certificate): CertificateTrustLevel {
        val issuerDn = certificate.issuerDN.name
        val isDebug = issuerDn.contains("CN=Android Debug") &&
            issuerDn.contains("O=Android") &&
            issuerDn.contains("C=US")
        return if (isDebug) CertificateTrustLevel.Debug else CertificateTrustLevel.Valid
    }
}
