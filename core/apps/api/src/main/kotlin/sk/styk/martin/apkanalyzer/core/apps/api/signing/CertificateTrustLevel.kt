package sk.styk.martin.apkanalyzer.core.apps.signing

enum class CertificateTrustLevel { Valid, Debug }

fun resolveCertificateTrustLevel(issuer: CertificatePrincipal): CertificateTrustLevel {
    val isDebug = issuer.name?.startsWith("Android Debug") == true &&
        issuer.organization?.startsWith("Android") == true &&
        issuer.country?.startsWith("US") == true
    return if (isDebug) CertificateTrustLevel.Debug else CertificateTrustLevel.Valid
}
