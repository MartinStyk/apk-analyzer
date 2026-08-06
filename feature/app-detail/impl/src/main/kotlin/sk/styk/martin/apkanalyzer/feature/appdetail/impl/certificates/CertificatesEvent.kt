package sk.styk.martin.apkanalyzer.feature.appdetail.impl.certificates

internal sealed interface CertificatesEvent {
    data object ShowCopiedFeedback : CertificatesEvent
}
