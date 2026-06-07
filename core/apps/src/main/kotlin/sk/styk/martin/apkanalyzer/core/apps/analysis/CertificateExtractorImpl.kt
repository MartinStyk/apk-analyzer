package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageInfo
import android.content.pm.Signature
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateData
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

    override fun getCertificateData(packageInfo: PackageInfo): List<CertificateData> = packageInfo.signingInfo?.apkContentsSigners.orEmpty()
        .mapNotNull { signature ->
            runCatching { signature.toCertificateData() }
                .onFailure { Logger.e(TAG, it, "Could not parse certificate for ${packageInfo.packageName}") }
                .getOrNull()
        }

    private fun Signature.toCertificateData(): CertificateData {
        val certificate = ByteArrayInputStream(toByteArray()).use { stream ->
            CertificateFactory.getInstance("X509").generateCertificate(stream) as X509Certificate
        }
        val publicKeyHex = digestManager.byteToHexString(certificate.publicKey.encoded)
        return CertificateData(
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
            issuerName = certificate.issuerX500Principal?.getField("CN=([^,]*)"),
            issuerOrganization = certificate.issuerX500Principal?.getField("O=([^,]*)"),
            issuerCountry = certificate.issuerX500Principal?.getField("C=([^,]*)"),
            subjectName = certificate.subjectX500Principal?.getField("CN=([^,]*)"),
            subjectOrganization = certificate.subjectX500Principal?.getField("O=([^,]*)"),
            subjectCountry = certificate.subjectX500Principal?.getField("C=([^,]*)"),
        )
    }

    private fun X500Principal.getField(pattern: String): String? = getName(RFC1779).takeUnless { it.isNullOrBlank() }?.let { Regex(pattern).find(it)?.groupValues?.get(1) }
}
