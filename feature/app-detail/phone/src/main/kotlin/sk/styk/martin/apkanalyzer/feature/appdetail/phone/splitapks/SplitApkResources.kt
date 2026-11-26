package sk.styk.martin.apkanalyzer.feature.appdetail.phone.splitapks

import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.SplitApkKind
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons

internal val SplitApkKind.icon: ImageVector
    get() = when (this) {
        SplitApkKind.DynamicFeature -> ApkAnalyzerIcons.Widgets
        SplitApkKind.Abi -> ApkAnalyzerIcons.Memory
        SplitApkKind.ScreenDensity -> ApkAnalyzerIcons.Screen
        SplitApkKind.Language -> ApkAnalyzerIcons.Language
    }
