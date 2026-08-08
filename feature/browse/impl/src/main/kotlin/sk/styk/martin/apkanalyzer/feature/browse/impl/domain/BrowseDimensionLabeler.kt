package sk.styk.martin.apkanalyzer.feature.browse.impl.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import sk.styk.martin.apkanalyzer.core.apppermissions.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.apps.analysis.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.apps.model.AppCategory
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

        BrowseDimension.SharedUserId -> key

        BrowseDimension.AppCategory -> categoryLabel(AppCategory.valueOf(key))

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

        BrowseDimension.SharedUserId -> null

        BrowseDimension.AppCategory -> null

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

    private fun categoryLabel(category: AppCategory): String = when (category) {
        AppCategory.Undefined -> context.getString(R.string.browse_category_undefined)
        AppCategory.Game -> context.getString(R.string.browse_category_game)
        AppCategory.Audio -> context.getString(R.string.browse_category_audio)
        AppCategory.Video -> context.getString(R.string.browse_category_video)
        AppCategory.Image -> context.getString(R.string.browse_category_image)
        AppCategory.Social -> context.getString(R.string.browse_category_social)
        AppCategory.News -> context.getString(R.string.browse_category_news)
        AppCategory.Maps -> context.getString(R.string.browse_category_maps)
        AppCategory.Productivity -> context.getString(R.string.browse_category_productivity)
        AppCategory.Accessibility -> context.getString(R.string.browse_category_accessibility)
    }
}

private fun formatFingerprint(hex: String): String = hex.uppercase().chunked(2).joinToString(":")

private fun String.splitToChunks(byteCount: Int): String {
    val bytes = split(":")
    val shortened = bytes.take(byteCount).joinToString(":")
    return if (bytes.size > byteCount) "$shortened…" else shortened
}
