package sk.styk.martin.apkanalyzer.core.appindex.api.browse

import sk.styk.martin.apkanalyzer.core.appindex.api.model.AppAttributeIndex
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

const val UNKNOWN_SIGNER_KEY = " unknown-signer"
const val UNKNOWN_COUNTRY_KEY = " unknown-country"

fun AppAttributeIndex.bucketsFor(dimension: BrowseDimension, subAttribute: BrowseSubAttribute? = null): Map<String, List<PackageName>> = when (dimension) {
    BrowseDimension.Permission -> permission

    BrowseDimension.TargetSdk -> targetSdk.mapKeys { it.key.toString() }

    BrowseDimension.MinSdk -> minSdk.mapKeys { it.key.toString() }

    BrowseDimension.InstallSource -> installSource.mapKeys { it.key.name }

    BrowseDimension.SharedUserId -> sharedUserId

    BrowseDimension.AppCategory -> appCategory.mapKeys { it.key.name }

    BrowseDimension.SigningCertificate -> when (subAttribute) {
        BrowseSubAttribute.CertificateSha1 -> certificateSha1
        BrowseSubAttribute.CertificateMd5 -> certificateMd5
        BrowseSubAttribute.CertificateOrganization -> certificateOrganization.mapKeys { it.key ?: UNKNOWN_SIGNER_KEY }
        BrowseSubAttribute.CertificateCountry -> certificateCountry.mapKeys { it.key ?: UNKNOWN_COUNTRY_KEY }
        BrowseSubAttribute.CertificateSha256, null -> certificateSha256
    }
}
