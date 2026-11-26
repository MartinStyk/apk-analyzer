package sk.styk.martin.apkanalyzer.feature.browse.tv

import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.permissionIcon

internal val BrowseDimension.icon: ImageVector
    get() = when (this) {
        BrowseDimension.Permission -> ApkAnalyzerIcons.Permissions
        BrowseDimension.SigningCertificate -> ApkAnalyzerIcons.Fingerprint
        BrowseDimension.TargetSdk, BrowseDimension.MinSdk -> ApkAnalyzerIcons.Android
        BrowseDimension.InstallSource -> ApkAnalyzerIcons.Folder
        BrowseDimension.SharedUserId -> ApkAnalyzerIcons.Group
        BrowseDimension.AppCategory -> ApkAnalyzerIcons.Widgets
    }

internal fun BrowseDimension.bucketIcon(bucketKey: String): ImageVector = when (this) {
    BrowseDimension.Permission -> permissionIcon(bucketKey, groupName = null)

    BrowseDimension.SigningCertificate,
    BrowseDimension.TargetSdk,
    BrowseDimension.MinSdk,
    BrowseDimension.InstallSource,
    BrowseDimension.SharedUserId,
    BrowseDimension.AppCategory,
    -> icon
}
