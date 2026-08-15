package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilterKey
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.intentfilters.IntentFiltersRepository
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.toAppReference

@HiltViewModel(assistedFactory = IntentFiltersViewModel.Factory::class)
internal class IntentFiltersViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    @Assisted private val componentName: String,
    @Assisted private val componentType: ComponentType,
    private val intentFiltersRepository: IntentFiltersRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val clipboardManager: ClipboardManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            appDetailInput: AppDetailInput,
            componentName: String,
            componentType: ComponentType,
        ): IntentFiltersViewModel
    }

    private val source = MutableStateFlow<IntentFiltersSource>(IntentFiltersSource.Loading)
    private val query = MutableStateFlow("")

    val state: StateFlow<IntentFiltersState> = combine(source, query) { source, query ->
        when (source) {
            IntentFiltersSource.Loading -> IntentFiltersState.Loading
            IntentFiltersSource.Error -> IntentFiltersState.Error
            IntentFiltersSource.Unavailable -> IntentFiltersState.Unavailable
            is IntentFiltersSource.Ready -> source.filteredBy(query, context)
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IntentFiltersState.Loading)

    private val eventChannel = Channel<IntentFiltersEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadFilters()
    }

    fun onAction(action: IntentFiltersAction) {
        when (action) {
            IntentFiltersAction.Retry -> loadFilters()

            IntentFiltersAction.Back -> eventChannel.trySend(IntentFiltersEvent.NavigateBack)

            IntentFiltersAction.ClearQuery -> query.value = ""

            is IntentFiltersAction.ChangeQuery -> query.value = action.query

            is IntentFiltersAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { eventChannel.send(IntentFiltersEvent.ShowCopiedFeedback) }
                }
            }
        }
    }

    private fun loadFilters() {
        source.value = IntentFiltersSource.Loading
        viewModelScope.launch {
            source.value = withContext(dispatcherProvider.default()) {
                intentFiltersRepository.componentIntentFilters(appDetailInput.toAppReference()).fold(
                    onSuccess = { filtersByComponent ->
                        val key = ComponentIntentFilterKey(componentName, componentType.toComponentKind())
                        IntentFiltersSource.Ready(componentName, componentType, filtersByComponent[key].orEmpty().toItems())
                    },
                    onFailure = { IntentFiltersSource.Unavailable },
                )
            }
        }
    }
}

private sealed interface IntentFiltersSource {
    data object Loading : IntentFiltersSource
    data object Error : IntentFiltersSource
    data object Unavailable : IntentFiltersSource
    data class Ready(
        val componentName: String,
        val componentType: ComponentType,
        val filters: ImmutableList<ComponentIntentFilterItem>,
    ) : IntentFiltersSource
}

private fun IntentFiltersSource.Ready.filteredBy(query: String, context: Context) = IntentFiltersState.Loaded(
    componentName = componentName,
    componentType = componentType,
    query = query,
    totalCount = filters.size,
    filters = filters.filter { it.matches(query, componentType, context) }.toImmutableList(),
)

private fun ComponentIntentFilterItem.matches(
    query: String,
    componentType: ComponentType,
    context: Context,
): Boolean {
    if (query.isBlank()) return true
    return searchableValues(componentType, context).any { it.contains(query, ignoreCase = true) }
}

private fun ComponentIntentFilterItem.searchableValues(componentType: ComponentType, context: Context): Sequence<String> = sequence {
    yield(context.getString(purposeTitleRes(componentType)))
    yieldAll(actions)
    yieldAll(categories)
    dataRules.forEach {
        yield(it.type.manifestAttribute)
        yield(it.value)
    }
    uriRelativeGroups.forEach { group ->
        yield(if (group.isAllowed) "allow" else "block")
        group.dataRules.forEach {
            yield(it.type.manifestAttribute)
            yield(it.value)
        }
    }
    if (isAutoVerify) {
        yield("autoVerify")
        yield(context.getString(R.string.intent_filters_tag_verified))
    }
    if (uriRelativeGroups.any { !it.isAllowed }) {
        yield(context.getString(R.string.intent_filters_tag_blocked_rules))
    }
    if (priority != 0) yield(priority.toString())
    if (order != 0) yield(order.toString())
}

private fun ComponentType.toComponentKind(): ComponentKind = when (this) {
    ComponentType.Activity -> ComponentKind.Activity
    ComponentType.Service -> ComponentKind.Service
    ComponentType.Receiver -> ComponentKind.Receiver
    ComponentType.Provider -> ComponentKind.Provider
}
