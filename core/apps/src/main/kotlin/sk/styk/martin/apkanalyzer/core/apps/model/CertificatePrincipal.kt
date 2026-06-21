package sk.styk.martin.apkanalyzer.core.apps.model

data class CertificatePrincipal(val name: String? = null, val organization: String? = null, val country: String? = null) {
    val displayName: String?
        get() = organization ?: name
}
