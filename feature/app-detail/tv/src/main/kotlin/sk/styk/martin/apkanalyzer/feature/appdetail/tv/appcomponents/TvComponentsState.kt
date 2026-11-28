package sk.styk.martin.apkanalyzer.feature.appdetail.tv.appcomponents

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentFlag
import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.api.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.api.components.ProviderPathPermission

@Immutable
internal data class TvComponentItem(
    val name: String,
    val kind: ComponentKind,
    val isExported: Boolean,
    val isUnprotected: Boolean,
    val isLauncher: Boolean,
    val isLaunchable: Boolean = false,
    val isDisabled: Boolean = false,
    val permission: String? = null,
    val readPermission: String? = null,
    val writePermission: String? = null,
    val authority: String? = null,
    val label: String? = null,
    val targetActivity: String? = null,
    val parentName: String? = null,
    val pathPermissions: List<ProviderPathPermission> = listOf(),
    val flags: List<ComponentFlag> = listOf(),
    val intentFilters: List<ComponentIntentFilter>? = listOf(),
) {
    val key: String
        get() = "${kind.name}:$name"

    val simpleName: String
        get() = name.substringAfterLast('.')
}

@Immutable
internal data class TvComponentSection(val kind: ComponentKind, val components: List<TvComponentItem>)

@Immutable
internal sealed interface TvComponentScope {
    data object All : TvComponentScope

    data class Only(val kind: ComponentKind) : TvComponentScope
}

@Immutable
internal data class TvComponentScopeOption(val scope: TvComponentScope, val count: Int)

@Immutable
internal sealed interface TvComponentsState {
    data object Loading : TvComponentsState

    data object Error : TvComponentsState

    @Immutable
    data class Loaded(
        val scope: TvComponentScope,
        val scopeOptions: List<TvComponentScopeOption>,
        val sections: List<TvComponentSection>,
    ) : TvComponentsState
}
