package sk.styk.martin.apkanalyzer.feature.appdetail.tv.appcomponents

import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons

internal val ComponentKind.icon: ImageVector
    get() = when (this) {
        ComponentKind.Activity -> ApkAnalyzerIcons.ComponentActivity
        ComponentKind.Service -> ApkAnalyzerIcons.ComponentService
        ComponentKind.Receiver -> ApkAnalyzerIcons.ComponentReceiver
        ComponentKind.Provider -> ApkAnalyzerIcons.ComponentProvider
    }
