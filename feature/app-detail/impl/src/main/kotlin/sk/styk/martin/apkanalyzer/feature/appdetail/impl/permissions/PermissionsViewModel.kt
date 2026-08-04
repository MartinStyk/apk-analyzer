package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

import android.content.pm.PermissionInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apppermissions.PermissionLabelProvider
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.Permission
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.io.File

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PermissionsState.Loading)

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

            is PermissionsAction.ToggleFilter -> narrowing.update { current ->
                val filters = if (action.filter in current.filters) {
                    current.filters - action.filter
                } else {
                    current.filters + action.filter
                }
                current.copy(filters = filters)
            }

            is PermissionsAction.ClearNarrowing -> narrowing.update { it.copy(query = "", filters = emptySet()) }

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
                when (appDetailInput) {
                    is AppDetailInput.InstalledPackage -> appDetailRepository.installedPackageDetails(appDetailInput.packageName)
                    is AppDetailInput.ApkFile -> appDetailRepository.apkFilePackageDetails(File(appDetailInput.apkFilePath))
                }.onFailure {
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
                        grantState = when {
                            !reportsGrantState -> null
                            used.isGranted -> GrantState.Granted
                            else -> GrantState.Denied
                        },
                    )
                }
                .sortedBy { it.label.lowercase() },
            defined = permissions.defined
                .map { it.toItem(grantState = null) }
                .sortedBy { it.label.lowercase() },
        )
    }

    private fun Permission.toItem(grantState: GrantState?) = PermissionItem(
        name = name,
        label = permissionLabelProvider.getLabel(name),
        description = permissionDescriptionProvider.describe(this),
        groupName = groupName,
        protectionLevel = protection.toProtectionLevel(),
        protectionFlags = protectionFlags.toProtectionFlags(),
        grantState = grantState,
        declaringPackage = declaringPackage,
    )
}

private sealed interface PermissionsSource {
    data object Loading : PermissionsSource
    data object Error : PermissionsSource
    data class Ready(val requested: List<PermissionItem>, val defined: List<PermissionItem>) : PermissionsSource
}

private data class Narrowing(val scope: PermissionScope = PermissionScope.Requested, val query: String = "", val filters: Set<PermissionFilter> = emptySet())

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
    val availableFilters = if (scope == PermissionScope.Defined) persistentListOf() else scoped.availableFilters()
    val activeFilters = narrowing.filters.intersect(availableFilters)
    val matching = scoped.filter { it.matches(narrowing.query) && it.satisfies(activeFilters) }

    return PermissionsState.Loaded(
        scope = scope,
        scopeOptions = scopeOptions,
        activeFilters = activeFilters.toImmutableSet(),
        availableFilters = availableFilters,
        query = narrowing.query,
        scopeTotal = scoped.size,
        sections = ProtectionLevel.entries
            .mapNotNull { level ->
                matching.filter { it.protectionLevel == level }
                    .takeIf { it.isNotEmpty() }
                    ?.let { PermissionSection(level, it.toImmutableList()) }
            }
            .toImmutableList(),
    )
}

private fun List<PermissionItem>.availableFilters(): ImmutableList<PermissionFilter> = when {
    any { it.grantState != null } -> persistentListOf(PermissionFilter.Dangerous, PermissionFilter.Granted, PermissionFilter.Denied)
    any { it.protectionLevel == ProtectionLevel.Dangerous } -> persistentListOf(PermissionFilter.Dangerous)
    else -> persistentListOf()
}

private fun PermissionItem.matches(query: String): Boolean = query.isBlank() ||
    name.contains(query, ignoreCase = true) ||
    label.contains(query, ignoreCase = true)

private fun PermissionItem.satisfies(filters: Set<PermissionFilter>): Boolean {
    if (PermissionFilter.Dangerous in filters && protectionLevel != ProtectionLevel.Dangerous) return false
    val requestedGrantStates = filters.mapNotNull { it.grantState }
    return requestedGrantStates.isEmpty() || grantState in requestedGrantStates
}

private val PermissionFilter.grantState: GrantState?
    get() = when (this) {
        PermissionFilter.Granted -> GrantState.Granted
        PermissionFilter.Denied -> GrantState.Denied
        PermissionFilter.Dangerous -> null
    }

@Suppress("DEPRECATION")
private fun Int.toProtectionLevel() = when (this) {
    PermissionInfo.PROTECTION_DANGEROUS -> ProtectionLevel.Dangerous
    PermissionInfo.PROTECTION_SIGNATURE, PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> ProtectionLevel.Signature
    PermissionInfo.PROTECTION_INTERNAL -> ProtectionLevel.Internal
    else -> ProtectionLevel.Normal
}

private fun Int.toProtectionFlags(): ImmutableList<ProtectionFlag> = protectionFlagsByMask
    .filter { (mask, _) -> this and mask != 0 }
    .values
    .toImmutableList()

private val protectionFlagsByMask = mapOf(
    PermissionInfo.PROTECTION_FLAG_PRIVILEGED to ProtectionFlag.Privileged,
    PermissionInfo.PROTECTION_FLAG_APPOP to ProtectionFlag.AppOp,
    PermissionInfo.PROTECTION_FLAG_INSTANT to ProtectionFlag.Instant,
    PermissionInfo.PROTECTION_FLAG_DEVELOPMENT to ProtectionFlag.Development,
)
