package sk.styk.martin.apkanalyzer.feature.appdetail.tv.appcomponents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.components.flags
import sk.styk.martin.apkanalyzer.core.apps.components.isExternallyReachableWithoutPermission
import sk.styk.martin.apkanalyzer.core.apps.components.isLaunchable
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSection
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.installedPackage

@HiltViewModel(assistedFactory = TvComponentsViewModel.Factory::class)
internal class TvComponentsViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val sectionRepository: AppDetailSectionRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): TvComponentsViewModel
    }

    private val source = MutableStateFlow<ComponentsSource>(ComponentsSource.Loading)
    private val scope = MutableStateFlow<TvComponentScope>(TvComponentScope.All)
    private val eventChannel = Channel<TvComponentsEvent>(Channel.BUFFERED)
    val events: Flow<TvComponentsEvent> = eventChannel.receiveAsFlow()

    val state: StateFlow<TvComponentsState> = combine(source, scope) { source, scope ->
        when (source) {
            ComponentsSource.Loading -> TvComponentsState.Loading
            ComponentsSource.Error -> TvComponentsState.Error
            is ComponentsSource.Ready -> source.toState(scope)
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvComponentsState.Loading)

    init {
        loadComponents()
    }

    fun onAction(action: TvComponentsAction) {
        when (action) {
            TvComponentsAction.Retry -> loadComponents()

            is TvComponentsAction.SelectScope -> scope.value = action.scope

            is TvComponentsAction.LaunchComponent -> {
                val packageName = (appDetailInput as? AppDetailInput.InstalledPackage)?.packageName?.let(::PackageName) ?: return
                eventChannel.trySend(TvComponentsEvent.LaunchComponent(packageName, action.className, action.kind))
            }
        }
    }

    private fun loadComponents() {
        source.value = ComponentsSource.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            val canLaunch = appDetailInput.installedPackage != null
            sectionRepository.detail(appDetailInput)
                .onSuccess { detail ->
                    source.value = detail.toSource(canLaunch, intentFiltersByComponent = null)
                    val intentFilters = (sectionRepository.intentFilters(appDetailInput).getOrNull() as? AppDetailSection.Available)?.value
                    source.value = detail.toSource(canLaunch, intentFiltersByComponent = intentFilters)
                }
                .onFailure { source.value = ComponentsSource.Error }
        }
    }
}

private sealed interface ComponentsSource {
    data object Loading : ComponentsSource
    data object Error : ComponentsSource
    data class Ready(val appName: String, val componentsByKind: Map<ComponentKind, List<TvComponentItem>>) : ComponentsSource
}

private fun AppDetail.toSource(
    canLaunch: Boolean,
    intentFiltersByComponent: Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>?,
): ComponentsSource.Ready {
    fun filtersFor(name: String, kind: ComponentKind) = intentFiltersByComponent?.let { it[ComponentIntentFilterKey(name, kind)].orEmpty() }
    return ComponentsSource.Ready(
        appName = info.applicationName,
        componentsByKind = mapOf(
            ComponentKind.Activity to activities.map { it.toItem(canLaunch, filtersFor(it.name, ComponentKind.Activity)) },
            ComponentKind.Service to services.map { it.toItem(filtersFor(it.name, ComponentKind.Service)) },
            ComponentKind.Receiver to receivers.map { it.toItem(canLaunch, filtersFor(it.name, ComponentKind.Receiver)) },
            ComponentKind.Provider to contentProviders.map { it.toItem(filtersFor(it.name, ComponentKind.Provider)) },
        ).mapValues { (_, items) -> items.mergeDuplicatesByKey().sortedForDisplay() },
    )
}

private fun Activity.toItem(canLaunch: Boolean, intentFilters: List<ComponentIntentFilter>?) = TvComponentItem(
    name = name,
    kind = ComponentKind.Activity,
    isExported = isExported,
    isUnprotected = isExternallyReachableWithoutPermission,
    isLauncher = isLauncher == true,
    isLaunchable = canLaunch && isLaunchable,
    permission = permission,
    label = label,
    targetActivity = targetActivity,
    parentName = parentName,
    intentFilters = intentFilters,
)

private fun Service.toItem(intentFilters: List<ComponentIntentFilter>?) = TvComponentItem(
    name = name,
    kind = ComponentKind.Service,
    isExported = isExported,
    isUnprotected = isExternallyReachableWithoutPermission,
    isLauncher = false,
    permission = permission,
    flags = flags,
    intentFilters = intentFilters,
)

private fun BroadcastReceiver.toItem(canLaunch: Boolean, intentFilters: List<ComponentIntentFilter>?) = TvComponentItem(
    name = name,
    kind = ComponentKind.Receiver,
    isExported = isExported,
    isUnprotected = isExternallyReachableWithoutPermission,
    isLauncher = false,
    isLaunchable = canLaunch && isLaunchable,
    permission = permission,
    intentFilters = intentFilters,
)

private fun ContentProvider.toItem(intentFilters: List<ComponentIntentFilter>?) = TvComponentItem(
    name = name,
    kind = ComponentKind.Provider,
    isExported = isExported,
    isUnprotected = isExternallyReachableWithoutPermission,
    isLauncher = false,
    readPermission = readPermission,
    writePermission = writePermission,
    authority = authority,
    pathPermissions = pathPermissions,
    intentFilters = intentFilters,
)

private fun List<TvComponentItem>.mergeDuplicatesByKey(): List<TvComponentItem> =
    groupBy { it.key }.values.map { duplicates -> duplicates.reduce(::moreExposed) }

private fun moreExposed(a: TvComponentItem, b: TvComponentItem): TvComponentItem = when {
    a.isExported != b.isExported -> if (a.isExported) a else b
    a.isUnprotected != b.isUnprotected -> if (a.isUnprotected) a else b
    else -> a
}

private fun List<TvComponentItem>.sortedForDisplay() = sortedWith(
    compareByDescending<TvComponentItem> { it.isLauncher }
        .thenByDescending { it.isExported }
        .thenBy { it.simpleName.lowercase() },
)

private fun ComponentsSource.Ready.toState(selectedScope: TvComponentScope): TvComponentsState.Loaded {
    val presentKinds = ComponentKind.entries.filter { componentsByKind[it].orEmpty().isNotEmpty() }
    val scopeOptions = buildList {
        add(TvComponentScopeOption(TvComponentScope.All, presentKinds.sumOf { componentsByKind[it].orEmpty().size }))
        presentKinds.forEach { add(TvComponentScopeOption(TvComponentScope.Only(it), componentsByKind[it].orEmpty().size)) }
    }
    val scope = selectedScope.takeIf { selected -> scopeOptions.any { it.scope == selected } } ?: TvComponentScope.All
    val kinds = when (scope) {
        TvComponentScope.All -> presentKinds
        is TvComponentScope.Only -> listOf(scope.kind)
    }
    return TvComponentsState.Loaded(
        appName = appName,
        scope = scope,
        scopeOptions = scopeOptions,
        sections = kinds.map { TvComponentSection(it, componentsByKind[it].orEmpty()) },
    )
}
