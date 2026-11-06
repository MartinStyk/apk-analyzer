package sk.styk.martin.apkanalyzer.core.apps.signing

import java.time.Instant

val ApkAnalyzerCertificate = Certificate(
    signAlgorithm = "SHA256withRSA",
    certificateHashMd5 = "a1".repeat(16),
    certificateHashSha1 = "a1".repeat(20),
    certificateHashSha256 = "a1".repeat(32),
    publicKeyMd5 = "a2".repeat(16),
    publicKeySha1 = "a2".repeat(20),
    publicKeySha256 = "a2".repeat(32),
    validFrom = Instant.parse("2017-03-14T10:00:00Z"),
    validUntil = Instant.parse("2047-03-07T10:00:00Z"),
    serialNumber = "1A:2B:3C:4D",
    issuer = CertificatePrincipal(name = "Martin Styk", organization = "Apk Analyzer", country = "SK"),
    subject = CertificatePrincipal(name = "Martin Styk", organization = "Apk Analyzer", country = "SK"),
    trustLevel = CertificateTrustLevel.Valid,
    isSelfSigned = true,
)

val WhatsAppCertificate = Certificate(
    signAlgorithm = "SHA1withRSA",
    certificateHashMd5 = "b1".repeat(16),
    certificateHashSha1 = "b1".repeat(20),
    certificateHashSha256 = "b1".repeat(32),
    publicKeyMd5 = "b2".repeat(16),
    publicKeySha1 = "b2".repeat(20),
    publicKeySha256 = "b2".repeat(32),
    validFrom = Instant.parse("2010-06-25T23:07:16Z"),
    validUntil = Instant.parse("2044-02-15T23:07:16Z"),
    serialNumber = "4C:2B:3A:1D",
    issuer = CertificatePrincipal(name = "WhatsApp", organization = "WhatsApp Inc.", country = "US"),
    subject = CertificatePrincipal(name = "WhatsApp", organization = "WhatsApp Inc.", country = "US"),
    trustLevel = CertificateTrustLevel.Valid,
    isSelfSigned = true,
)

val GoogleCertificate = Certificate(
    signAlgorithm = "SHA256withRSA",
    certificateHashMd5 = "c1".repeat(16),
    certificateHashSha1 = "c1".repeat(20),
    certificateHashSha256 = "c1".repeat(32),
    publicKeyMd5 = "c2".repeat(16),
    publicKeySha1 = "c2".repeat(20),
    publicKeySha256 = "c2".repeat(32),
    validFrom = Instant.parse("2008-08-21T23:13:34Z"),
    validUntil = Instant.parse("2036-01-07T23:13:34Z"),
    serialNumber = "00:C2:E0:87:46",
    issuer = CertificatePrincipal(name = "Android", organization = "Google Inc.", country = "US"),
    subject = CertificatePrincipal(name = "Android", organization = "Google Inc.", country = "US"),
    trustLevel = CertificateTrustLevel.Valid,
    isSelfSigned = true,
)
