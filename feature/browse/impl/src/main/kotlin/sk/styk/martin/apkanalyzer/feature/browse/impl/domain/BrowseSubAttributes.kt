package sk.styk.martin.apkanalyzer.feature.browse.impl.domain

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.feature.browse.impl.R
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

internal const val CERTIFICATE_FINGERPRINT = "fingerprint"
internal const val CERTIFICATE_ORGANIZATION = "organization"
internal const val CERTIFICATE_COUNTRY = "country"

internal data class BrowseSubAttribute(val key: String, val labelRes: Int)

internal fun BrowseDimension.subAttributes(): ImmutableList<BrowseSubAttribute> = when (this) {
    BrowseDimension.SigningCertificate -> persistentListOf(
        BrowseSubAttribute(CERTIFICATE_FINGERPRINT, R.string.browse_certificate_attribute_fingerprint),
        BrowseSubAttribute(CERTIFICATE_ORGANIZATION, R.string.browse_certificate_attribute_organization),
        BrowseSubAttribute(CERTIFICATE_COUNTRY, R.string.browse_certificate_attribute_country),
    )

    BrowseDimension.Permission,
    BrowseDimension.TargetSdk,
    BrowseDimension.MinSdk,
    BrowseDimension.InstallSource,
    -> persistentListOf()
}
