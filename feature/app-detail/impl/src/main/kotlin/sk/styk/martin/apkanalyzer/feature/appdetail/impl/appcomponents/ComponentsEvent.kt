package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal sealed interface ComponentsEvent {
    data object ShowCopiedFeedback : ComponentsEvent
    data class LaunchComponent(
        val packageName: PackageName,
        val className: String,
        val type: ComponentType,
    ) : ComponentsEvent
}
