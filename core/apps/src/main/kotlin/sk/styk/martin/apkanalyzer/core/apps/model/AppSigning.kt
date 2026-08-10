package sk.styk.martin.apkanalyzer.core.apps.model

data class AppSigning(
    val currentCertificates: List<Certificate> = emptyList(),
    val pastCertificates: List<Certificate> = emptyList(),
    val hasMultipleSigners: Boolean = false,
    val signingSchemeVersions: List<SigningSchemeVersion>? = null,
)
