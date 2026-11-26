package sk.styk.martin.apkanalyzer.core.appindex.api.browse

import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.appindex.api.R

enum class BrowseSubAttribute(@StringRes val labelRes: Int) {
    CertificateSha256(R.string.browse_certificate_attribute_sha256),
    CertificateSha1(R.string.browse_certificate_attribute_sha1),
    CertificateMd5(R.string.browse_certificate_attribute_md5),
    CertificateOrganization(R.string.browse_certificate_attribute_organization),
    CertificateCountry(R.string.browse_certificate_attribute_country),
}

fun BrowseDimension.subAttributes(): List<BrowseSubAttribute> = when (this) {
    BrowseDimension.SigningCertificate -> listOf(
        BrowseSubAttribute.CertificateSha256,
        BrowseSubAttribute.CertificateSha1,
        BrowseSubAttribute.CertificateMd5,
        BrowseSubAttribute.CertificateOrganization,
        BrowseSubAttribute.CertificateCountry,
    )

    BrowseDimension.Permission,
    BrowseDimension.TargetSdk,
    BrowseDimension.MinSdk,
    BrowseDimension.InstallSource,
    BrowseDimension.SharedUserId,
    BrowseDimension.AppCategory,
    -> listOf()
}

val BrowseSubAttribute?.isCertificateHash: Boolean
    get() = when (this) {
        BrowseSubAttribute.CertificateSha256,
        BrowseSubAttribute.CertificateSha1,
        BrowseSubAttribute.CertificateMd5,
        -> true

        BrowseSubAttribute.CertificateOrganization,
        BrowseSubAttribute.CertificateCountry,
        null,
        -> false
    }

@get:StringRes
val BrowseSubAttribute.cardLabelRes: Int
    get() = when (this) {
        BrowseSubAttribute.CertificateSha256 -> R.string.browse_certificate_hash_sha256

        BrowseSubAttribute.CertificateSha1 -> R.string.browse_certificate_hash_sha1

        BrowseSubAttribute.CertificateMd5 -> R.string.browse_certificate_hash_md5

        BrowseSubAttribute.CertificateOrganization,
        BrowseSubAttribute.CertificateCountry,
        -> labelRes
    }
