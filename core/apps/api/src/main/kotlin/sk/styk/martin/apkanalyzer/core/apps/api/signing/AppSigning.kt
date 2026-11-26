package sk.styk.martin.apkanalyzer.core.apps.api.signing

data class AppSigning(
    val currentCertificates: List<Certificate> = emptyList(),
    val pastCertificates: List<Certificate> = emptyList(),
    val hasMultipleSigners: Boolean = false,
)
