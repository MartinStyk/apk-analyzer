package sk.styk.martin.apkanalyzer.core.appanalysis

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.appanalysis.model.CertificateData
import sk.styk.martin.apkanalyzer.core.common.digest.DigestManager
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.regex.Pattern
import javax.inject.Inject
import javax.security.auth.x500.X500Principal
import javax.security.auth.x500.X500Principal.RFC1779

class CertificateManager
@Inject
internal constructor(private val digestManager: DigestManager) {
    fun getCertificateData(packageInfo: PackageInfo): CertificateData {
        val signature = packageInfo.signingInfo?.apkContentsSigners?.firstOrNull() ?: throw IllegalStateException("No signature")

        return ByteArrayInputStream(signature.toByteArray()).use {
            val certFactory = CertificateFactory.getInstance("X509")
            val certificate = certFactory.generateCertificate(it) as X509Certificate

            CertificateData(
                signAlgorithm = certificate.sigAlgName,
                certificateHashMd5 = digestManager.md5Digest(certificate.encoded),
                certificateHashSha1 = digestManager.sha1Digest(certificate.encoded),
                certificateHashSha256 = digestManager.sha256Digest(certificate.encoded),
                publicKeyMd5 = digestManager.md5Digest(digestManager.byteToHexString(certificate.publicKey.encoded)),
                publicKeySha1 = digestManager.sha1Digest(digestManager.byteToHexString(certificate.publicKey.encoded)),
                publicKeySha256 = digestManager.sha256Digest(digestManager.byteToHexString(certificate.publicKey.encoded)),
                startDate = certificate.notBefore,
                endDate = certificate.notAfter,
                serialNumber = certificate.serialNumber.toInt(),
                issuerName = certificate.issuerX500Principal?.getPrincipalCommonName(),
                issuerOrganization = certificate.issuerX500Principal?.getPrincipalOrganization(),
                issuerCountry = certificate.issuerX500Principal?.getPrincipalCountry(),
                subjectName = certificate.subjectX500Principal?.getPrincipalCommonName(),
                subjectOrganization = certificate.subjectX500Principal?.getPrincipalOrganization(),
                subjectCountry = certificate.subjectX500Principal?.getPrincipalCountry(),
            )
        }
    }

    fun getSignAlgorithm(packageInfo: PackageInfo): String? {
        val signature = packageInfo.signingInfo?.apkContentsSigners?.firstOrNull() ?: return null

        ByteArrayInputStream(signature.toByteArray()).use {
            try {
                val certFactory = CertificateFactory.getInstance("X509")
                val certificate = certFactory.generateCertificate(it) as X509Certificate
                return certificate.sigAlgName
            } catch (e: Exception) {
                Logger.e(TAG_APP_ANALYSIS, e, "Could not get sign algo for $packageInfo.")
            }
        }
        return null
    }

    private fun X500Principal.getPrincipalCommonName(): String? {
        val name = getName(RFC1779)
        return if (name.isNullOrBlank()) null else parsePrincipal(name, "CN=([^,]*)")
    }

    private fun X500Principal.getPrincipalOrganization(): String? {
        val name = getName(RFC1779)
        return if (name.isNullOrBlank()) null else parsePrincipal(name, "O=([^,]*)")
    }

    private fun X500Principal.getPrincipalCountry(): String? {
        val name: String? = getName(RFC1779)
        return if (name.isNullOrBlank()) null else parsePrincipal(name, "C=([^,]*)")
    }

    private fun parsePrincipal(principalName: String, patternString: String): String? {
        val matcher = Pattern.compile(patternString).matcher(principalName)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
}
