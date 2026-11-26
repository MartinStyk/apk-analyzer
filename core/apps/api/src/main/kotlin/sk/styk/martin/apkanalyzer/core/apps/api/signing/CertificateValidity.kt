package sk.styk.martin.apkanalyzer.core.apps.signing

import java.time.Instant

enum class CertificateValidity {
    Valid,
    Expired,
    NotYetValid,
}

fun Certificate.validityAt(now: Instant): CertificateValidity = when {
    now.isBefore(validFrom) -> CertificateValidity.NotYetValid
    now.isAfter(validUntil) -> CertificateValidity.Expired
    else -> CertificateValidity.Valid
}
