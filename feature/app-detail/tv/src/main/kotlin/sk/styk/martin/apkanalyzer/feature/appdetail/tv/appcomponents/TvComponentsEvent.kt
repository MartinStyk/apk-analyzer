package sk.styk.martin.apkanalyzer.feature.appdetail.tv.appcomponents

import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

internal sealed interface TvComponentsEvent {
    data class LaunchComponent(
        val packageName: PackageName,
        val className: String,
        val kind: ComponentKind,
    ) : TvComponentsEvent
}
