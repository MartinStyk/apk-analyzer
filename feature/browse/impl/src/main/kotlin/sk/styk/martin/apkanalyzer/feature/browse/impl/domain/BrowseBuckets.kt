package sk.styk.martin.apkanalyzer.feature.browse.impl.domain

import sk.styk.martin.apkanalyzer.core.appindex.model.AppAttributeIndex
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

internal const val UNKNOWN_SIGNER_KEY = " unknown-signer"
internal const val UNKNOWN_COUNTRY_KEY = " unknown-country"

internal fun AppAttributeIndex.bucketsFor(dimension: BrowseDimension, subKey: String? = null): Map<String, List<PackageName>> = when (dimension) {
    BrowseDimension.Permission -> permission

    BrowseDimension.TargetSdk -> targetSdk.mapKeys { it.key.toString() }

    BrowseDimension.MinSdk -> minSdk.mapKeys { it.key.toString() }

    BrowseDimension.InstallSource -> installSource.mapKeys { it.key.name }

    BrowseDimension.SharedUserId -> sharedUserId

    BrowseDimension.AppCategory -> appCategory.mapKeys { it.key.name }

    BrowseDimension.SigningCertificate -> when (subKey) {
        CERTIFICATE_ORGANIZATION -> certificateOrganization.mapKeys { it.key ?: UNKNOWN_SIGNER_KEY }
        CERTIFICATE_COUNTRY -> certificateCountry.mapKeys { it.key ?: UNKNOWN_COUNTRY_KEY }
        else -> certificateFingerprint
    }
}
