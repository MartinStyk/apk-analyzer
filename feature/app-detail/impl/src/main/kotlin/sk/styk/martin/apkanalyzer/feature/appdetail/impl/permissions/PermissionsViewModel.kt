package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.channels.Channel
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
import sk.styk.martin.apkanalyzer.core.apppermissions.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.toAppReference

private const val TAG = "PermissionsViewModel"

@HiltViewModel(assistedFactory = PermissionsViewModel.Factory::class)
internal class PermissionsViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val clipboardManager: ClipboardManager,
    private val permissionLabelProvider: PermissionLabelProvider,
    private val permissionDescriptionProvider: PermissionDescriptionProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): PermissionsViewModel
    }

    private val source = MutableStateFlow<PermissionsSource>(PermissionsSource.Loading)
    private val narrowing = MutableStateFlow(Narrowing())

    val state: StateFlow<PermissionsState> = combine(source, narrowing) { source, narrowing ->
        when (source) {
            is PermissionsSource.Loading -> PermissionsState.Loading
            is PermissionsSource.Error -> PermissionsState.Error
            is PermissionsSource.Ready -> source.narrowedBy(narrowing)
        }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PermissionsState.Loading)

    private val eventChannel = Channel<PermissionsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadPermissions()
    }

    fun onAction(action: PermissionsAction) {
        when (action) {
            is PermissionsAction.Retry -> loadPermissions()

            is PermissionsAction.ChangeQuery -> narrowing.update { it.copy(query = action.query) }

            is PermissionsAction.SelectScope -> narrowing.update { it.copy(scope = action.scope) }

            is PermissionsAction.ToggleProtectionLevel -> narrowing.update { current ->
                current.copy(protectionLevels = current.protectionLevels.toggled(action.protectionLevel))
            }

            is PermissionsAction.ToggleGrantState -> narrowing.update { current ->
                current.copy(grantStates = current.grantStates.toggled(action.grantState))
            }

            is PermissionsAction.ClearNarrowing -> narrowing.update {
                it.copy(query = "", protectionLevels = emptySet(), grantStates = emptySet())
            }

            is PermissionsAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { eventChannel.send(PermissionsEvent.ShowCopiedFeedback) }
                }
            }
        }
    }

    private fun loadPermissions() {
        source.value = PermissionsSource.Loading
        viewModelScope.launch {
            source.value = withContext(dispatcherProvider.default()) {
                appDetailRepository.details(appDetailInput.toAppReference()).onFailure {
                    Logger.e(TAG, it, "Can not load permissions for $appDetailInput")
                }.fold(
                    onSuccess = { it.toSource() },
                    onFailure = { PermissionsSource.Error },
                )
            }
        }
    }

    private fun AppDetail.toSource(): PermissionsSource.Ready {
        val reportsGrantState = appDetailInput is AppDetailInput.InstalledPackage
        return PermissionsSource.Ready(
            requested = permissions.used
                .map { used ->
                    used.permissionData.toItem(
                        analysedPackage = info.packageName,
                        grantState = when {
                            !reportsGrantState -> null
                            used.isGranted -> GrantState.Granted
                            else -> GrantState.NotGranted
                        },
                    )
                }
                .sortedBy { it.label.lowercase() },
            defined = permissions.defined
                .map { it.toItem(analysedPackage = info.packageName, grantState = null) }
                .sortedBy { it.label.lowercase() },
        )
    }

    private fun Permission.toItem(analysedPackage: PackageName, grantState: GrantState?) = PermissionItem(
        name = name,
        label = permissionLabelProvider.getLabel(name),
        description = permissionDescriptionProvider.describe(this),
        groupName = details?.groupName,
        protectionLevel = details?.protectionLevel,
        protectionFlags = details?.protectionFlags.orEmpty().toImmutableList(),
        grantState = grantState,
        declaringPackage = details?.declaringPackage,
        isSelfDeclared = details?.declaringPackage == analysedPackage,
    )
}

private sealed interface PermissionsSource {
    data object Loading : PermissionsSource
    data object Error : PermissionsSource
    data class Ready(val requested: List<PermissionItem>, val defined: List<PermissionItem>) : PermissionsSource
}

private val orderedProtectionLevels: List<ProtectionLevel?> = ProtectionLevel.entries + null

private data class Narrowing(
    val scope: PermissionScope = PermissionScope.Requested,
    val query: String = "",
    val protectionLevels: Set<ProtectionLevel?> = emptySet(),
    val grantStates: Set<GrantState> = emptySet(),
)

private fun PermissionsSource.Ready.narrowedBy(narrowing: Narrowing): PermissionsState.Loaded {
    val scopeOptions = if (defined.isEmpty()) {
        persistentListOf(PermissionScope.Requested)
    } else {
        persistentListOf(PermissionScope.Requested, PermissionScope.Defined)
    }
    val scope = narrowing.scope.takeIf { it in scopeOptions } ?: PermissionScope.Requested
    val scoped = when (scope) {
        PermissionScope.Requested -> requested
        PermissionScope.Defined -> defined
    }
    val presentProtectionLevels = scoped.mapTo(mutableSetOf()) { it.protectionLevel }
    val protectionLevelOptions = orderedProtectionLevels
        .filter { it in presentProtectionLevels }
        .toImmutableList()
    val grantStateOptions = if (scoped.any { it.grantState != null }) {
        GrantState.entries.toImmutableList()
    } else {
        persistentListOf()
    }
    val selectedProtectionLevels = narrowing.protectionLevels.intersect(protectionLevelOptions)
    val selectedGrantStates = narrowing.grantStates.intersect(grantStateOptions)
    val matching = scoped.filter {
        it.matches(narrowing.query) &&
            it.satisfies(
                protectionLevels = selectedProtectionLevels,
                grantStates = selectedGrantStates,
            )
    }

    return PermissionsState.Loaded(
        scope = scope,
        scopeOptions = scopeOptions,
        selectedProtectionLevels = selectedProtectionLevels.toImmutableSet(),
        protectionLevelOptions = protectionLevelOptions,
        selectedGrantStates = selectedGrantStates.toImmutableSet(),
        grantStateOptions = grantStateOptions,
        query = narrowing.query,
        scopeTotal = scoped.size,
        sections = orderedProtectionLevels
            .mapNotNull { level ->
                matching.filter { it.protectionLevel == level }
                    .takeIf { it.isNotEmpty() }
                    ?.let { PermissionSection(level, it.toImmutableList()) }
            }
            .toImmutableList(),
    )
}

private fun PermissionItem.matches(query: String): Boolean = query.isBlank() ||
    name.contains(query, ignoreCase = true) ||
    label.contains(query, ignoreCase = true)

private fun PermissionItem.satisfies(protectionLevels: Set<ProtectionLevel?>, grantStates: Set<GrantState>): Boolean {
    if (protectionLevels.isNotEmpty() && protectionLevel !in protectionLevels) return false
    return grantStates.isEmpty() || grantState in grantStates
}

private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value
