package sk.styk.martin.apkanalyzer.feature.browse.impl.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import sk.styk.martin.apkanalyzer.core.apppermissions.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.apps.analysis.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.feature.browse.impl.R
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension
import java.util.Locale
import javax.inject.Inject

internal class BrowseDimensionLabeler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionLabelProvider: PermissionLabelProvider,
    private val sdkVersionResolver: SdkVersionResolver,
) {

    fun label(
        dimension: BrowseDimension,
        key: String,
        subAttribute: BrowseSubAttribute? = null,
    ): String = when (dimension) {
        BrowseDimension.Permission -> permissionLabelProvider.getLabel(key)

        BrowseDimension.TargetSdk, BrowseDimension.MinSdk ->
            sdkVersionResolver.resolveVersion(key.toIntOrNull()) ?: context.getString(R.string.browse_sdk_unknown, key)

        BrowseDimension.InstallSource -> when (key) {
            AppSource.GooglePlay.name -> context.getString(R.string.browse_source_google_play)
            AppSource.SystemPreinstalled.name -> context.getString(R.string.browse_source_system)
            else -> context.getString(R.string.browse_source_unknown)
        }

        BrowseDimension.SigningCertificate -> when (subAttribute) {
            BrowseSubAttribute.CertificateOrganization ->
                key.takeUnless { it == UNKNOWN_SIGNER_KEY } ?: context.getString(R.string.browse_signer_unknown)

            BrowseSubAttribute.CertificateCountry ->
                key.takeUnless { it == UNKNOWN_COUNTRY_KEY }?.let { countryLabel(it) } ?: context.getString(R.string.browse_country_unknown)

            BrowseSubAttribute.CertificateSha256,
            BrowseSubAttribute.CertificateSha1,
            BrowseSubAttribute.CertificateMd5,
            null,
            -> formatFingerprint(key)
        }
    }

    fun rawIdentifier(
        dimension: BrowseDimension,
        key: String,
        subAttribute: BrowseSubAttribute? = null,
    ): String? = when (dimension) {
        BrowseDimension.Permission -> key

        BrowseDimension.TargetSdk, BrowseDimension.MinSdk -> context.getString(R.string.browse_api_level, key)

        BrowseDimension.InstallSource -> null

        BrowseDimension.SigningCertificate -> when (subAttribute) {
            BrowseSubAttribute.CertificateCountry -> key.takeUnless { it == UNKNOWN_COUNTRY_KEY }

            BrowseSubAttribute.CertificateOrganization,
            BrowseSubAttribute.CertificateSha256,
            BrowseSubAttribute.CertificateSha1,
            BrowseSubAttribute.CertificateMd5,
            null,
            -> null
        }
    }

    private fun countryLabel(countryCode: String): String {
        val displayName = runCatching { Locale.Builder().setRegion(countryCode).build().displayCountry }.getOrNull()
        return displayName?.takeIf { it.isNotBlank() && !it.equals(countryCode, ignoreCase = true) } ?: countryCode
    }
}

private fun formatFingerprint(hex: String): String = hex.uppercase().chunked(2).joinToString(":")
