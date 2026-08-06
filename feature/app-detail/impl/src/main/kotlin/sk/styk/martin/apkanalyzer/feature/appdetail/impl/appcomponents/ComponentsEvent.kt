package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

internal sealed interface ComponentsEvent {
    data object ShowCopiedFeedback : ComponentsEvent
    data class LaunchComponent(val packageName: String, val className: String, val type: ComponentType) : ComponentsEvent
}
