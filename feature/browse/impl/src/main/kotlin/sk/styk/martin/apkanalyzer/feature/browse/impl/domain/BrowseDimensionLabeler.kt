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

        BrowseDimension.InstallSource -> AppSource.valueOf(key).installSourceLabel()

        BrowseDimension.SharedUserId -> key

        BrowseDimension.AppCategory -> AppCategory.valueOf(key).categoryLabel()

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

        BrowseDimension.SharedUserId -> null

        BrowseDimension.AppCategory -> null

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

    private fun AppSource.installSourceLabel(): String = when (this) {
        AppSource.GooglePlay -> context.getString(R.string.browse_source_google_play)
        AppSource.SamsungGalaxyStore -> context.getString(R.string.browse_source_samsung_galaxy_store)
        AppSource.AmazonAppstore -> context.getString(R.string.browse_source_amazon_appstore)
        AppSource.HuaweiAppGallery -> context.getString(R.string.browse_source_huawei_app_gallery)
        AppSource.XiaomiGetApps -> context.getString(R.string.browse_source_xiaomi_get_apps)
        AppSource.FDroid -> context.getString(R.string.browse_source_fdroid)
        AppSource.AuroraStore -> context.getString(R.string.browse_source_aurora_store)
        AppSource.Sideloaded -> context.getString(R.string.browse_source_sideloaded)
        AppSource.LocalInstall -> context.getString(R.string.browse_source_local_install)
        AppSource.SystemPreinstalled -> context.getString(R.string.browse_source_system)
        AppSource.Unknown -> context.getString(R.string.browse_source_unknown)
    }

    private fun AppCategory.categoryLabel(): String = when (this) {
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
