package sk.styk.martin.apkanalyzer.feature.browse.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

internal fun BrowseDimension.icon(): ImageVector = when (this) {
    BrowseDimension.Permission -> ApkAnalyzerIcons.Permissions
    BrowseDimension.SigningCertificate -> ApkAnalyzerIcons.Fingerprint
    BrowseDimension.TargetSdk, BrowseDimension.MinSdk -> ApkAnalyzerIcons.Android
    BrowseDimension.InstallSource -> ApkAnalyzerIcons.Folder
}

@Composable
internal fun BrowseDimension.title(): String = stringResource(
    when (this) {
        BrowseDimension.Permission -> R.string.browse_dimension_permission_title
        BrowseDimension.SigningCertificate -> R.string.browse_dimension_certificate_title
        BrowseDimension.TargetSdk -> R.string.browse_dimension_target_sdk_title
        BrowseDimension.MinSdk -> R.string.browse_dimension_min_sdk_title
        BrowseDimension.InstallSource -> R.string.browse_dimension_install_source_title
    },
)

@Composable
internal fun BrowseDimension.subtitle(): String = stringResource(
    when (this) {
        BrowseDimension.Permission -> R.string.browse_dimension_permission_subtitle
        BrowseDimension.SigningCertificate -> R.string.browse_dimension_certificate_subtitle
        BrowseDimension.TargetSdk -> R.string.browse_dimension_target_sdk_subtitle
        BrowseDimension.MinSdk -> R.string.browse_dimension_min_sdk_subtitle
        BrowseDimension.InstallSource -> R.string.browse_dimension_install_source_subtitle
    },
)
