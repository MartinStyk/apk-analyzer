package sk.styk.martin.apkanalyzer.feature.browse.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.subtitleRes
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.titleRes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons

internal fun BrowseDimension.icon(): ImageVector = when (this) {
    BrowseDimension.Permission -> ApkAnalyzerIcons.Permissions
    BrowseDimension.SigningCertificate -> ApkAnalyzerIcons.Fingerprint
    BrowseDimension.TargetSdk, BrowseDimension.MinSdk -> ApkAnalyzerIcons.Android
    BrowseDimension.InstallSource -> ApkAnalyzerIcons.Folder
    BrowseDimension.SharedUserId -> ApkAnalyzerIcons.Group
    BrowseDimension.AppCategory -> ApkAnalyzerIcons.Widgets
}

@Composable
internal fun BrowseDimension.title(): String = stringResource(titleRes)

@Composable
internal fun BrowseDimension.subtitle(): String = stringResource(subtitleRes)
