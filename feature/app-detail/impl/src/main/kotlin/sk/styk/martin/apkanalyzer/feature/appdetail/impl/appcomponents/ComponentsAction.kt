package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

internal sealed interface ComponentsAction {
    data object Retry : ComponentsAction
    data object ClearNarrowing : ComponentsAction
    data class ChangeQuery(val query: String) : ComponentsAction
    data class SelectScope(val scope: ComponentScope) : ComponentsAction
    data class ToggleFilter(val filter: ComponentFilter) : ComponentsAction
    data class CopyValue(val label: String, val value: String) : ComponentsAction
    data class LaunchComponent(val className: String, val type: ComponentType) : ComponentsAction
}
