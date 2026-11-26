package sk.styk.martin.apkanalyzer.feature.appdetail.tv.certificates

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.api.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.api.signing.CertificateValidity
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeVersion

@Immutable
internal data class TvCertificate(val certificate: Certificate, val validity: CertificateValidity)

@Immutable
internal sealed interface TvCertificatesState {
    data object Loading : TvCertificatesState

    data object Error : TvCertificatesState

    @Immutable
    data class Loaded(
        val currentCertificates: List<TvCertificate>,
        val pastCertificates: List<TvCertificate>,
        val hasMultipleSigners: Boolean,
        val signingSchemeVersions: List<SigningSchemeVersion>?,
    ) : TvCertificatesState
}
