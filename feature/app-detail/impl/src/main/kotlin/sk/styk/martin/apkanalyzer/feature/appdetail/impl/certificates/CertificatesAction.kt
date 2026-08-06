package sk.styk.martin.apkanalyzer.feature.appdetail.impl.certificates

internal sealed interface CertificatesAction {
    data object Retry : CertificatesAction
    data class CopyValue(val label: String, val value: String) : CertificatesAction
}
