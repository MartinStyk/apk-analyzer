package sk.styk.martin.apkanalyzer.core.apps.signing

import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.apps.R

val CertificatePrincipal.displayLine: String?
    get() = listOfNotNull(
        organization?.takeIf { it.isNotBlank() },
        name?.takeIf { it.isNotBlank() },
        country?.takeIf { it.isNotBlank() },
    ).distinct().takeIf { it.isNotEmpty() }?.joinToString(" · ")

@get:StringRes
val CertificateValidity.labelRes: Int
    get() = when (this) {
        CertificateValidity.Valid -> R.string.certificates_valid
        CertificateValidity.Expired -> R.string.certificates_expired
        CertificateValidity.NotYetValid -> R.string.certificates_not_yet_valid
    }

@get:StringRes
val CertificateValidity.explanationRes: Int
    get() = when (this) {
        CertificateValidity.Valid -> R.string.certificates_valid_explanation
        CertificateValidity.Expired -> R.string.certificates_expired_explanation
        CertificateValidity.NotYetValid -> R.string.certificates_not_yet_valid_explanation
    }

@get:StringRes
val CertificateValidity.onRecordedDateLabelRes: Int
    get() = when (this) {
        CertificateValidity.Valid -> R.string.certificates_validity_on_date_valid
        CertificateValidity.Expired -> R.string.certificates_validity_on_date_expired
        CertificateValidity.NotYetValid -> R.string.certificates_validity_on_date_not_yet
    }

@get:StringRes
val CertificateValidity.onRecordedDateExplanationRes: Int
    get() = when (this) {
        CertificateValidity.Valid -> R.string.certificates_validity_on_date_valid_explanation
        CertificateValidity.Expired -> R.string.certificates_validity_on_date_expired_explanation
        CertificateValidity.NotYetValid -> R.string.certificates_validity_on_date_not_yet_explanation
    }

@get:StringRes
val SignatureAlgorithmAssessment.labelRes: Int
    get() = when (this) {
        SignatureAlgorithmAssessment.ModernDigest -> R.string.certificates_algorithm_strong

        SignatureAlgorithmAssessment.WeakSha1Digest,
        SignatureAlgorithmAssessment.WeakMd5Digest,
        SignatureAlgorithmAssessment.WeakMd2Digest,
        -> R.string.certificates_algorithm_weak

        SignatureAlgorithmAssessment.Unknown -> R.string.certificates_algorithm_unknown
    }

@get:StringRes
val SignatureAlgorithmAssessment.explanationRes: Int
    get() = when (this) {
        SignatureAlgorithmAssessment.ModernDigest -> R.string.certificates_algorithm_strong_explanation
        SignatureAlgorithmAssessment.WeakSha1Digest -> R.string.certificates_algorithm_sha1_explanation
        SignatureAlgorithmAssessment.WeakMd5Digest -> R.string.certificates_algorithm_md5_explanation
        SignatureAlgorithmAssessment.WeakMd2Digest -> R.string.certificates_algorithm_md2_explanation
        SignatureAlgorithmAssessment.Unknown -> R.string.certificates_algorithm_unknown_explanation
    }

val SignatureAlgorithmAssessment.isWeak: Boolean
    get() = this == SignatureAlgorithmAssessment.WeakSha1Digest ||
        this == SignatureAlgorithmAssessment.WeakMd5Digest ||
        this == SignatureAlgorithmAssessment.WeakMd2Digest

@get:StringRes
val SigningSchemeVersion.labelRes: Int
    get() = when (this) {
        SigningSchemeVersion.V1 -> R.string.certificates_scheme_v1
        SigningSchemeVersion.V2 -> R.string.certificates_scheme_v2
        SigningSchemeVersion.V3 -> R.string.certificates_scheme_v3
        SigningSchemeVersion.V3_1 -> R.string.certificates_scheme_v3_1
    }
