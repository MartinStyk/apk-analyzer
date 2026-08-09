package sk.styk.martin.apkanalyzer.feature.browse.impl.domain

import androidx.annotation.StringRes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.feature.browse.impl.R
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

@Serializable
internal enum class BrowseSubAttribute(@StringRes val labelRes: Int) {
    CertificateSha256(R.string.browse_certificate_attribute_sha256),
    CertificateSha1(R.string.browse_certificate_attribute_sha1),
    CertificateMd5(R.string.browse_certificate_attribute_md5),
    CertificateOrganization(R.string.browse_certificate_attribute_organization),
    CertificateCountry(R.string.browse_certificate_attribute_country),
}

internal fun BrowseDimension.subAttributes(): ImmutableList<BrowseSubAttribute> = when (this) {
    BrowseDimension.SigningCertificate -> persistentListOf(
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
    -> persistentListOf()
}

internal val BrowseSubAttribute?.isCertificateHash: Boolean
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
internal val BrowseSubAttribute.cardLabelRes: Int
    get() = when (this) {
        BrowseSubAttribute.CertificateSha256 -> R.string.browse_certificate_hash_sha256

        BrowseSubAttribute.CertificateSha1 -> R.string.browse_certificate_hash_sha1

        BrowseSubAttribute.CertificateMd5 -> R.string.browse_certificate_hash_md5

        BrowseSubAttribute.CertificateOrganization,
        BrowseSubAttribute.CertificateCountry,
        -> labelRes
    }
