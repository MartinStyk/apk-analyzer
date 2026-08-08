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

private const val FINGERPRINT_LABEL_BYTE_COUNT = 8

internal class BrowseDimensionLabeler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionLabelProvider: PermissionLabelProvider,
    private val sdkVersionResolver: SdkVersionResolver,
) {

    fun label(
        dimension: BrowseDimension,
        key: String,
        subKey: String? = null,
    ): String = when (dimension) {
        BrowseDimension.Permission -> permissionLabelProvider.getLabel(key)

        BrowseDimension.TargetSdk, BrowseDimension.MinSdk ->
            sdkVersionResolver.resolveVersion(key.toIntOrNull()) ?: context.getString(R.string.browse_sdk_unknown, key)

        BrowseDimension.InstallSource -> when (key) {
            AppSource.GooglePlay.name -> context.getString(R.string.browse_source_google_play)
            AppSource.SystemPreinstalled.name -> context.getString(R.string.browse_source_system)
            else -> context.getString(R.string.browse_source_unknown)
        }

        BrowseDimension.SigningCertificate -> when (subKey) {
            CERTIFICATE_ORGANIZATION -> key.takeUnless { it == UNKNOWN_SIGNER_KEY } ?: context.getString(R.string.browse_signer_unknown)

            CERTIFICATE_COUNTRY ->
                key.takeUnless { it == UNKNOWN_COUNTRY_KEY }?.let { countryLabel(it) } ?: context.getString(R.string.browse_country_unknown)

            else -> formatFingerprint(key).splitToChunks(FINGERPRINT_LABEL_BYTE_COUNT)
        }
    }

    fun rawIdentifier(
        dimension: BrowseDimension,
        key: String,
        subKey: String? = null,
    ): String? = when (dimension) {
        BrowseDimension.Permission -> key

        BrowseDimension.TargetSdk, BrowseDimension.MinSdk -> context.getString(R.string.browse_api_level, key)

        BrowseDimension.InstallSource -> null

        BrowseDimension.SigningCertificate -> when (subKey) {
            CERTIFICATE_ORGANIZATION -> null
            CERTIFICATE_COUNTRY -> key.takeUnless { it == UNKNOWN_COUNTRY_KEY }
            else -> formatFingerprint(key)
        }
    }

    private fun countryLabel(countryCode: String): String {
        val displayName = runCatching { Locale.Builder().setRegion(countryCode).build().displayCountry }.getOrNull()
        return displayName?.takeIf { it.isNotBlank() && !it.equals(countryCode, ignoreCase = true) } ?: countryCode
    }
}

private fun formatFingerprint(hex: String): String = hex.uppercase().chunked(2).joinToString(":")

private fun String.splitToChunks(byteCount: Int): String {
    val bytes = split(":")
    val shortened = bytes.take(byteCount).joinToString(":")
    return if (bytes.size > byteCount) "$shortened…" else shortened
}
