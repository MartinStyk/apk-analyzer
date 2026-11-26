package sk.styk.martin.apkanalyzer.feature.appdetail.phone.requirements

import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.RequirementItem
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.requirementIcon

internal val RequirementItem.icon: ImageVector
    get() = when (this) {
        is RequirementItem.Hardware -> requirementIcon(name)
        is RequirementItem.OpenGlEs -> ApkAnalyzerIcons.Graphics
    }
