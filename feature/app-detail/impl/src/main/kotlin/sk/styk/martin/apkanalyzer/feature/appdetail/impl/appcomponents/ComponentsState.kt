package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import sk.styk.martin.apkanalyzer.core.apps.components.IntentFilterDataRuleType
import sk.styk.martin.apkanalyzer.core.apps.components.ProviderPathMatchType

@Serializable
internal enum class ComponentScope {
    All,
    Activities,
    Services,
    Receivers,
    Providers,
}

@Serializable
internal enum class ComponentFilter {
    Exported,
    Unprotected,
}

@Serializable
internal enum class ComponentType {
    Activity,
    Service,
    Receiver,
    Provider,
}

internal enum class ComponentFlag {
    StopWithTask,
    SingleUser,
    IsolatedProcess,
    ExternalService,
}

@Immutable
internal sealed interface ComponentDetails {

    @Immutable
    data class ActivityDetails(
        val label: String?,
        val targetActivity: String?,
        val parentName: String?,
        val permission: String?,
        val isLauncher: Boolean?,
    ) : ComponentDetails

    @Immutable
    data class ServiceDetails(val permission: String?) : ComponentDetails

    @Immutable
    data class ReceiverDetails(val permission: String?) : ComponentDetails

    @Immutable
    data class ProviderDetails(
        val authority: String?,
        val readPermission: String?,
        val writePermission: String?,
        val pathPermissions: ImmutableList<ProviderPathPermissionItem>,
    ) : ComponentDetails
}

@Immutable
internal data class ProviderPathPermissionItem(
    val path: String,
    val matchType: ProviderPathMatchType,
    val readPermission: String?,
    val writePermission: String?,
)

@Immutable
internal data class ComponentItem(
    val name: String,
    val simpleName: String,
    val packagePath: String,
    val type: ComponentType,
    val isExported: Boolean,
    val isGuarded: Boolean,
    val isUnprotected: Boolean,
    val isLaunchable: Boolean,
    val flags: ImmutableList<ComponentFlag>,
    val intentFilters: ImmutableList<ComponentIntentFilterItem> = persistentListOf(),
    val areIntentFiltersAvailable: Boolean = true,
    val details: ComponentDetails,
) {
    val stableKey: String
        get() = "${type.name}:$name"

    val isLauncher: Boolean
        get() = (details as? ComponentDetails.ActivityDetails)?.isLauncher == true
}

@Immutable
internal data class ComponentIntentFilterItem(
    val index: Int,
    val actions: ImmutableList<String>,
    val categories: ImmutableList<String>,
    val dataRules: ImmutableList<IntentFilterDataRuleItem>,
    val uriRelativeGroups: ImmutableList<IntentFilterUriRelativeGroupItem>,
    val priority: Int,
    val order: Int,
    val isAutoVerify: Boolean,
)

@Immutable
internal data class IntentFilterUriRelativeGroupItem(val isAllowed: Boolean, val dataRules: ImmutableList<IntentFilterDataRuleItem>)

@Immutable
internal data class IntentFilterDataRuleItem(val type: IntentFilterDataRuleType, val value: String)

@Immutable
internal data class ComponentSection(val type: ComponentType, val components: ImmutableList<ComponentItem>)

@Immutable
internal sealed interface ComponentsState {
    data object Loading : ComponentsState

    data object Error : ComponentsState

    @Immutable
    data class Loaded(
        val scope: ComponentScope,
        val scopeOptions: ImmutableList<ComponentScope>,
        val selectedFilters: ImmutableSet<ComponentFilter>,
        val query: String,
        val scopeTotal: Int,
        val sections: ImmutableList<ComponentSection>,
    ) : ComponentsState {
        val hasResults: Boolean
            get() = sections.isNotEmpty()

        val isNarrowed: Boolean
            get() = query.isNotBlank() || selectedFilters.isNotEmpty()

        val isSectioned: Boolean
            get() = scope == ComponentScope.All
    }
}
