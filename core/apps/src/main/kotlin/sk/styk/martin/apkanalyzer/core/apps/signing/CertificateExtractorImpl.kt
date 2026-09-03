package sk.styk.martin.apkanalyzer.core.apps.signing

import android.content.pm.PackageInfo
import android.content.pm.Signature
import sk.styk.martin.apkanalyzer.core.common.digest.DigestManager
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale
import javax.inject.Inject
import javax.security.auth.x500.X500Principal

private const val TAG = "CertificateExtractorImpl"
private val unescapedCommaRegex = Regex("""(?<!\\),""")
private val escapedCharRegex = Regex("""\\(.)""")

internal class CertificateExtractorImpl @Inject constructor(private val digestManager: DigestManager) : CertificateExtractor {

    override fun getAppSigning(packageInfo: PackageInfo): AppSigning {
        val signingInfo = packageInfo.signingInfo ?: return AppSigning()
        val hasMultipleSigners = signingInfo.hasMultipleSigners()
        val currentSignatures = signingInfo.apkContentsSigners.orEmpty()
        val pastSignatures = if (hasMultipleSigners) {
            emptyList()
        } else {
            val currentSignerSet = currentSignatures.toSet()
            signingInfo.signingCertificateHistory.orEmpty().filterNot(currentSignerSet::contains)
        }

        return AppSigning(
            currentCertificates = currentSignatures.toList().toCertificates(packageInfo.packageName),
            pastCertificates = pastSignatures.toCertificates(packageInfo.packageName),
            hasMultipleSigners = hasMultipleSigners,
        )
    }

    private fun List<Signature>.toCertificates(packageName: String) = mapNotNull { signature ->
        runCatching { signature.toCertificateData() }
            .onFailure { Logger.w(TAG, it, "Could not parse certificate for $packageName") }
            .getOrNull()
    }

    private fun Signature.toCertificateData(): Certificate {
        val certificate = ByteArrayInputStream(toByteArray()).use { stream ->
            CertificateFactory.getInstance("X509").generateCertificate(stream) as X509Certificate
        }
        val publicKey = certificate.publicKey.encoded

        return Certificate(
            signAlgorithm = certificate.sigAlgName,
            certificateHashMd5 = digestManager.md5Digest(certificate.encoded),
            certificateHashSha1 = digestManager.sha1Digest(certificate.encoded),
            certificateHashSha256 = digestManager.sha256Digest(certificate.encoded),
            publicKeyMd5 = digestManager.md5Digest(publicKey),
            publicKeySha1 = digestManager.sha1Digest(publicKey),
            publicKeySha256 = digestManager.sha256Digest(publicKey),
            validFrom = certificate.notBefore.toInstant(),
            validUntil = certificate.notAfter.toInstant(),
            serialNumber = certificate.serialNumber.toColonSeparatedHex(),
            issuer = certificate.issuerX500Principal.toPrincipal(),
            subject = certificate.subjectX500Principal.toPrincipal(),
            trustLevel = resolveTrustLevel(certificate),
            isSelfSigned = certificate.isSelfSigned(),
        )
    }

    private fun X509Certificate.isSelfSigned(): Boolean {
        if (issuerX500Principal != subjectX500Principal) return false
        return try {
            verify(publicKey)
            true
        } catch (_: GeneralSecurityException) {
            false
        }
    }

    private fun X500Principal.toPrincipal(): CertificatePrincipal {
        val fields = name.toDistinguishedNameFields()
        return CertificatePrincipal(
            name = fields["CN"],
            organization = fields["O"],
            country = fields["C"],
        )
    }

    private fun String.toDistinguishedNameFields(): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        for (rdn in unescapedCommaRegex.split(this)) {
            val (type, value) = rdn.split('=', limit = 2).takeIf { it.size == 2 } ?: continue
            fields.putIfAbsent(type.trim().uppercase(Locale.ROOT), value.unescapeDnValue())
        }
        return fields
    }

    private fun String.unescapeDnValue(): String = escapedCharRegex.replace(this) { it.groupValues[1] }.trim()

    private fun resolveTrustLevel(certificate: X509Certificate): CertificateTrustLevel {
        val issuerDn = certificate.issuerX500Principal.name
        val isDebug = issuerDn.contains("CN=Android Debug") &&
            issuerDn.contains("O=Android") &&
            issuerDn.contains("C=US")
        return if (isDebug) CertificateTrustLevel.Debug else CertificateTrustLevel.Valid
    }

    private fun BigInteger.toColonSeparatedHex(): String {
        val hex = toString(16).uppercase(Locale.ROOT).let { if (it.length % 2 == 0) it else "0$it" }
        return hex.chunked(2).joinToString(":")
    }
}
