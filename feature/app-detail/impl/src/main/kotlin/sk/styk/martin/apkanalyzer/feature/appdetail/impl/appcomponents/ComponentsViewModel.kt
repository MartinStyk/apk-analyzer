package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.components.hasUnguardedPathPermission
import sk.styk.martin.apkanalyzer.core.apps.components.isExternallyReachableWithoutPermission
import sk.styk.martin.apkanalyzer.core.apps.intentfilters.IntentFiltersRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.toAppReference

@HiltViewModel(assistedFactory = ComponentsViewModel.Factory::class)
internal class ComponentsViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    @Assisted initialScope: ComponentScope,
    @Assisted initialFilters: Set<ComponentFilter>,
    private val appDetailRepository: AppDetailRepository,
    private val intentFiltersRepository: IntentFiltersRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            target: AppDetailInput,
            initialScope: ComponentScope,
            initialFilters: Set<ComponentFilter>,
        ): ComponentsViewModel
    }

    private val source = MutableStateFlow<ComponentsSource>(ComponentsSource.Loading)
    private val narrowing = MutableStateFlow(Narrowing(scope = initialScope, filters = initialFilters))

    val state: StateFlow<ComponentsState> = combine(source, narrowing) { source, narrowing ->
        when (source) {
            is ComponentsSource.Loading -> ComponentsState.Loading
            is ComponentsSource.Error -> ComponentsState.Error
            is ComponentsSource.Ready -> source.narrowedBy(narrowing)
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ComponentsState.Loading)

    private val eventChannel = Channel<ComponentsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadComponents()
    }

    fun onAction(action: ComponentsAction) {
        when (action) {
            is ComponentsAction.Retry -> loadComponents()

            is ComponentsAction.ChangeQuery -> narrowing.update { it.copy(query = action.query) }

            is ComponentsAction.SelectScope -> narrowing.update { it.copy(scope = action.scope) }

            is ComponentsAction.ToggleFilter -> narrowing.update { current ->
                current.copy(filters = current.filters.toggled(action.filter))
            }

            is ComponentsAction.ClearNarrowing -> narrowing.update { it.copy(query = "", filters = emptySet()) }

            is ComponentsAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { eventChannel.send(ComponentsEvent.ShowCopiedFeedback) }
                }
            }

            is ComponentsAction.LaunchComponent -> {
                val packageName = (appDetailInput as? AppDetailInput.InstalledPackage)?.packageName?.let(::PackageName) ?: return
                viewModelScope.launch {
                    eventChannel.send(ComponentsEvent.LaunchComponent(packageName, action.className, action.type))
                }
            }
        }
    }

    private fun loadComponents() {
        source.value = ComponentsSource.Loading
        viewModelScope.launch {
            val reference = appDetailInput.toAppReference()
            source.value = withContext(dispatcherProvider.default()) {
                coroutineScope {
                    val detailDeferred = async { appDetailRepository.details(reference) }
                    val filtersDeferred = async { intentFiltersRepository.componentIntentFilters(reference) }
                    val filtersResult = filtersDeferred.await()
                    detailDeferred.await().fold(
                        onSuccess = { detail ->
                            detail.toSource(
                                canLaunch = appDetailInput is AppDetailInput.InstalledPackage,
                                intentFiltersByComponent = filtersResult.getOrDefault(emptyMap()),
                                areIntentFiltersAvailable = filtersResult.isSuccess,
                            )
                        },
                        onFailure = { ComponentsSource.Error },
                    )
                }
            }
        }
    }
}

private sealed interface ComponentsSource {
    data object Loading : ComponentsSource
    data object Error : ComponentsSource
    data class Ready(val componentsByType: Map<ComponentType, List<ComponentItem>>) : ComponentsSource
}

private data class Narrowing(
    val scope: ComponentScope,
    val query: String = "",
    val filters: Set<ComponentFilter> = emptySet(),
)

private fun AppDetail.toSource(
    canLaunch: Boolean,
    intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>,
    areIntentFiltersAvailable: Boolean,
) = ComponentsSource.Ready(
    componentsByType = mapOf(
        ComponentType.Activity to activities.map {
            it.toItem(canLaunch, intentFiltersByComponent[ComponentIntentFilterKey(it.name, ComponentKind.Activity)].orEmpty(), areIntentFiltersAvailable)
        }.sortedForDisplay(),
        ComponentType.Service to services.map {
            it.toItem(intentFiltersByComponent[ComponentIntentFilterKey(it.name, ComponentKind.Service)].orEmpty(), areIntentFiltersAvailable)
        }.sortedForDisplay(),
        ComponentType.Receiver to receivers.map {
            it.toItem(canLaunch, intentFiltersByComponent[ComponentIntentFilterKey(it.name, ComponentKind.Receiver)].orEmpty(), areIntentFiltersAvailable)
        }.sortedForDisplay(),
        ComponentType.Provider to contentProviders.map {
            it.toItem(intentFiltersByComponent[ComponentIntentFilterKey(it.name, ComponentKind.Provider)].orEmpty(), areIntentFiltersAvailable)
        }.sortedForDisplay(),
    ),
)

private fun List<ComponentItem>.sortedForDisplay() = sortedWith(
    compareByDescending<ComponentItem> { it.isLauncher }
        .thenByDescending { it.isExported }
        .thenBy { it.simpleName.lowercase() },
)

private fun Activity.toItem(
    canLaunch: Boolean,
    intentFilters: List<ComponentIntentFilter>,
    areIntentFiltersAvailable: Boolean,
) = ComponentItem(
    name = name,
    simpleName = name.substringAfterLast('.'),
    packagePath = name.substringBeforeLast('.', ""),
    type = ComponentType.Activity,
    isExported = isExported,
    isGuarded = !permission.isNullOrBlank(),
    isUnprotected = isExternallyReachableWithoutPermission,
    isLaunchable = canLaunch && isExported && permission.isNullOrBlank(),
    flags = emptyImmutableFlags,
    intentFilters = intentFilters.toItems(),
    areIntentFiltersAvailable = areIntentFiltersAvailable,
    details = ComponentDetails.ActivityDetails(
        label = label,
        targetActivity = targetActivity,
        parentName = parentName,
        permission = permission,
        isLauncher = isLauncher,
    ),
)

private fun Service.toItem(intentFilters: List<ComponentIntentFilter>, areIntentFiltersAvailable: Boolean) = ComponentItem(
    name = name,
    simpleName = name.substringAfterLast('.'),
    packagePath = name.substringBeforeLast('.', ""),
    type = ComponentType.Service,
    isExported = isExported,
    isGuarded = !permission.isNullOrBlank(),
    isUnprotected = isExternallyReachableWithoutPermission,
    isLaunchable = false,
    flags = buildList {
        if (isStopWithTask) add(ComponentFlag.StopWithTask)
        if (isSingleUser) add(ComponentFlag.SingleUser)
        if (isIsolatedProcess) add(ComponentFlag.IsolatedProcess)
        if (isExternalService) add(ComponentFlag.ExternalService)
    }.toImmutableList(),
    intentFilters = intentFilters.toItems(),
    areIntentFiltersAvailable = areIntentFiltersAvailable,
    details = ComponentDetails.ServiceDetails(permission = permission),
)

private fun BroadcastReceiver.toItem(
    canLaunch: Boolean,
    intentFilters: List<ComponentIntentFilter>,
    areIntentFiltersAvailable: Boolean,
) = ComponentItem(
    name = name,
    simpleName = name.substringAfterLast('.'),
    packagePath = name.substringBeforeLast('.', ""),
    type = ComponentType.Receiver,
    isExported = isExported,
    isGuarded = !permission.isNullOrBlank(),
    isUnprotected = isExternallyReachableWithoutPermission,
    isLaunchable = canLaunch && isExported && permission.isNullOrBlank(),
    flags = emptyImmutableFlags,
    intentFilters = intentFilters.toItems(),
    areIntentFiltersAvailable = areIntentFiltersAvailable,
    details = ComponentDetails.ReceiverDetails(permission = permission),
)

private fun ContentProvider.toItem(intentFilters: List<ComponentIntentFilter>, areIntentFiltersAvailable: Boolean) = ComponentItem(
    name = name,
    simpleName = name.substringAfterLast('.'),
    packagePath = name.substringBeforeLast('.', ""),
    type = ComponentType.Provider,
    isExported = isExported,
    isGuarded = !readPermission.isNullOrBlank() && !writePermission.isNullOrBlank() && !hasUnguardedPathPermission,
    isUnprotected = isExternallyReachableWithoutPermission,
    isLaunchable = false,
    flags = emptyImmutableFlags,
    intentFilters = intentFilters.toItems(),
    areIntentFiltersAvailable = areIntentFiltersAvailable,
    details = ComponentDetails.ProviderDetails(
        authority = authority,
        readPermission = readPermission,
        writePermission = writePermission,
        pathPermissions = pathPermissions.map {
            ProviderPathPermissionItem(
                path = it.path,
                matchType = it.matchType,
                readPermission = it.readPermission,
                writePermission = it.writePermission,
            )
        }.toImmutableList(),
    ),
)

private val emptyImmutableFlags = emptyList<ComponentFlag>().toImmutableList()

private val scopeTypes = mapOf(
    ComponentScope.Activities to ComponentType.Activity,
    ComponentScope.Services to ComponentType.Service,
    ComponentScope.Receivers to ComponentType.Receiver,
    ComponentScope.Providers to ComponentType.Provider,
)

private fun ComponentsSource.Ready.narrowedBy(narrowing: Narrowing): ComponentsState.Loaded {
    val scopeOptions = buildList {
        add(ComponentScope.All)
        scopeTypes.forEach { (scope, type) ->
            if (componentsByType[type].orEmpty().isNotEmpty()) add(scope)
        }
    }.toImmutableList()
    val scope = narrowing.scope.takeIf { it in scopeOptions } ?: ComponentScope.All
    val scopedTypes = when (scope) {
        ComponentScope.All -> ComponentType.entries
        else -> listOfNotNull(scopeTypes[scope])
    }
    val scopeTotal = scopedTypes.sumOf { componentsByType[it].orEmpty().size }
    val sections = scopedTypes
        .mapNotNull { type ->
            componentsByType[type].orEmpty()
                .filter { it.matches(narrowing.query) && it.satisfies(narrowing.filters) }
                .takeIf { it.isNotEmpty() }
                ?.let { ComponentSection(type, it.toImmutableList()) }
        }
        .toImmutableList()

    return ComponentsState.Loaded(
        scope = scope,
        scopeOptions = scopeOptions,
        selectedFilters = narrowing.filters.toImmutableSet(),
        query = narrowing.query,
        scopeTotal = scopeTotal,
        sections = sections,
    )
}

private fun ComponentItem.matches(query: String): Boolean = query.isBlank() ||
    simpleName.contains(query, ignoreCase = true) ||
    packagePath.contains(query, ignoreCase = true)

private fun ComponentItem.satisfies(filters: Set<ComponentFilter>): Boolean = filters.all { filter ->
    when (filter) {
        ComponentFilter.Exported -> isExported
        ComponentFilter.Unprotected -> isUnprotected
    }
}

private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value
