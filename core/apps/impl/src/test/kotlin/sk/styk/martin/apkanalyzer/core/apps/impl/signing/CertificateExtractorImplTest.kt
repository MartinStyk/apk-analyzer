package sk.styk.martin.apkanalyzer.core.apps.signing

import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import sk.styk.martin.apkanalyzer.core.common.digest.DigestManager
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Instant
import javax.security.auth.x500.X500Principal
import java.security.Signature as JavaSignature

class CertificateExtractorImplTest {

    private val extractor: CertificateExtractor = CertificateExtractorImpl(DigestManager())

    @Test
    fun `Given no signing info When getting signing Then signing is empty`() {
        val packageInfo = aPackageInfoWithoutSigningInfo()

        val signing = extractor.getAppSigning(packageInfo)

        signing shouldBe AppSigning()
    }

    @Test
    fun `Given all name fields When getting signing Then subject contains each field`() {
        val packageInfo = aPackageInfo(aSignature(subject = "CN=Android,O=Google Inc,C=US"))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().subject shouldBe CertificatePrincipal(name = "Android", organization = "Google Inc", country = "US")
    }

    @Test
    fun `Given an organization with a comma When getting signing Then organization keeps the comma`() {
        val packageInfo = aPackageInfo(aSignature(subject = """O=Google\, Inc,C=US"""))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().subject shouldBe CertificatePrincipal(organization = "Google, Inc", country = "US")
    }

    @Test
    fun `Given an organization with quotes When getting signing Then organization keeps the quotes`() {
        val packageInfo = aPackageInfo(aSignature(subject = """O=\"Acme\",C=US"""))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().subject shouldBe CertificatePrincipal(organization = "\"Acme\"", country = "US")
    }

    @Test
    fun `Given an organization with a plus When getting signing Then organization keeps the plus`() {
        val packageInfo = aPackageInfo(aSignature(subject = """O=Acme\+Sons,C=US"""))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().subject shouldBe CertificatePrincipal(organization = "Acme+Sons", country = "US")
    }

    @Test
    fun `Given an organization ending in a backslash When getting signing Then country is still separated`() {
        val packageInfo = aPackageInfo(aSignature(subject = """O=Acme\\,C=US"""))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().subject shouldBe CertificatePrincipal(organization = """Acme\""", country = "US")
    }

    @Test
    fun `Given a multi-valued name When getting signing Then each value maps to its field`() {
        val packageInfo = aPackageInfo(aSignature(subject = "CN=Jane+O=Acme,C=US"))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().subject shouldBe CertificatePrincipal(name = "Jane", organization = "Acme", country = "US")
    }

    @Test
    fun `Given an organizational unit When getting signing Then organization ignores it`() {
        val packageInfo = aPackageInfo(aSignature(subject = "OU=Platform,O=Acme"))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().subject shouldBe CertificatePrincipal(organization = "Acme")
    }

    @Test
    fun `Given an Android debug issuer When getting signing Then trust level is debug`() {
        val packageInfo = aPackageInfo(aSignature(subject = "CN=Android Debug,O=Android,C=US"))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().trustLevel shouldBe CertificateTrustLevel.Debug
    }

    @Test
    fun `Given a different issuer When getting signing Then certificate is not self-signed`() {
        val packageInfo = aPackageInfo(aSignature(subject = "CN=App", issuer = "CN=Authority"))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().isSelfSigned shouldBe false
    }

    @Test
    fun `Given a matching issuer and own key signature When getting signing Then certificate is self-signed`() {
        val packageInfo = aPackageInfo(aSignature(subject = "CN=App"))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().isSelfSigned shouldBe true
    }

    @Test
    fun `Given a matching issuer and another key signature When getting signing Then certificate is not self-signed`() {
        val packageInfo = aPackageInfo(aSignature(subject = "CN=App", signedByOwnKey = false))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().isSelfSigned shouldBe false
    }

    @Test
    fun `Given an odd-length serial number When getting signing Then serial number is padded and colon separated`() {
        val packageInfo = aPackageInfo(aSignature(serialNumber = 0xABCDE))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().serialNumber shouldBe "0A:BC:DE"
    }

    @Test
    fun `Given a certificate When getting signing Then validity dates are preserved`() {
        val packageInfo = aPackageInfo(aSignature())

        val signing = extractor.getAppSigning(packageInfo)

        val certificate = signing.currentCertificates.single()
        certificate.validFrom shouldBe Instant.parse("2023-01-01T00:00:00Z")
        certificate.validUntil shouldBe Instant.parse("2033-01-01T00:00:00Z")
    }

    @Test
    fun `Given a rotated signer When getting signing Then past certificates exclude the current signer`() {
        val currentSigner = aSignature(subject = "CN=Current")
        val previousSigner = aSignature(subject = "CN=Previous")
        val packageInfo = aPackageInfo(currentSigner, signerHistory = listOf(previousSigner, currentSigner))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().subject.name shouldBe "Current"
        signing.pastCertificates.single().subject.name shouldBe "Previous"
    }

    @Test
    fun `Given multiple signers When getting signing Then all are current and none are past`() {
        val firstSigner = aSignature(subject = "CN=First")
        val secondSigner = aSignature(subject = "CN=Second")
        val packageInfo = aPackageInfo(firstSigner, secondSigner, signerHistory = listOf(aSignature(subject = "CN=Previous")))

        val signing = extractor.getAppSigning(packageInfo)

        signing.hasMultipleSigners shouldBe true
        signing.currentCertificates shouldHaveSize 2
        signing.pastCertificates.shouldBeEmpty()
    }

    @Test
    fun `Given an unparseable signature When getting signing Then only valid certificates remain`() {
        val packageInfo = aPackageInfo(anUnparseableSignature(), aSignature(subject = "CN=Valid"))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().subject.name shouldBe "Valid"
    }
}

private fun aPackageInfo(vararg currentSigners: Signature, signerHistory: List<Signature> = emptyList()): PackageInfo {
    val signing = mockk<SigningInfo> {
        every { hasMultipleSigners() } returns (currentSigners.size > 1)
        every { apkContentsSigners } returns arrayOf(*currentSigners)
        every { signingCertificateHistory } returns signerHistory.toTypedArray()
    }
    return mockk<PackageInfo>().apply {
        packageName = "com.example.app"
        signingInfo = signing
    }
}

private fun aPackageInfoWithoutSigningInfo(): PackageInfo = mockk()

private fun aSignature(
    subject: String = "CN=Test",
    issuer: String = subject,
    serialNumber: Long = 1,
    signedByOwnKey: Boolean = true,
): Signature {
    val certificate = certificateBytes(subject, issuer, serialNumber, signedByOwnKey)
    return mockk { every { toByteArray() } returns certificate }
}

private fun anUnparseableSignature(): Signature = mockk { every { toByteArray() } returns byteArrayOf(1, 2, 3) }

private const val DER_INTEGER = 0x02
private const val DER_BIT_STRING = 0x03
private const val DER_UTC_TIME = 0x17
private const val DER_SEQUENCE = 0x30
private val sha256WithRsaAlgorithm = byteArrayOf(
    0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B, 0x05, 0x00,
)
private val ownKeys by lazy { newKeyPair() }
private val otherKeys by lazy { newKeyPair() }

private fun certificateBytes(
    subject: String,
    issuer: String,
    serialNumber: Long,
    signedByOwnKey: Boolean,
): ByteArray {
    val tbsCertificate = der(
        DER_SEQUENCE,
        der(DER_INTEGER, BigInteger.valueOf(serialNumber).toByteArray()),
        sha256WithRsaAlgorithm,
        X500Principal(issuer).encoded,
        der(DER_SEQUENCE, der(DER_UTC_TIME, "230101000000Z".toByteArray()), der(DER_UTC_TIME, "330101000000Z".toByteArray())),
        X500Principal(subject).encoded,
        ownKeys.public.encoded,
    )
    val signature = JavaSignature.getInstance("SHA256withRSA").run {
        initSign(if (signedByOwnKey) ownKeys.private else otherKeys.private)
        update(tbsCertificate)
        sign()
    }
    return der(DER_SEQUENCE, tbsCertificate, sha256WithRsaAlgorithm, der(DER_BIT_STRING, byteArrayOf(0), signature))
}

private fun newKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

private fun der(tag: Int, vararg parts: ByteArray): ByteArray {
    val content = parts.fold(ByteArray(0), ByteArray::plus)
    return byteArrayOf(tag.toByte()) + derLength(content.size) + content
}

private fun derLength(length: Int): ByteArray = when {
    length < 0x80 -> byteArrayOf(length.toByte())
    length < 0x100 -> byteArrayOf(0x81.toByte(), length.toByte())
    else -> byteArrayOf(0x82.toByte(), (length shr 8).toByte(), length.toByte())
}
