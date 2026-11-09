package sk.styk.martin.apkanalyzer.feature.appdetail.tv.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItemMapper
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionScope
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository

@HiltViewModel(assistedFactory = TvPermissionsViewModel.Factory::class)
internal class TvPermissionsViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    @Assisted private val initialPermissionName: String?,
    private val sectionRepository: AppDetailSectionRepository,
    private val permissionItemMapper: PermissionItemMapper,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput, initialPermissionName: String?): TvPermissionsViewModel
    }

    private val source = MutableStateFlow<PermissionsSource>(PermissionsSource.Loading)
    private val scope = MutableStateFlow(PermissionScope.Requested)

    val state: StateFlow<TvPermissionsState> = combine(source, scope) { source, scope ->
        when (source) {
            PermissionsSource.Loading -> TvPermissionsState.Loading
            PermissionsSource.Error -> TvPermissionsState.Error
            is PermissionsSource.Ready -> source.toState(scope, initialPermissionName)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvPermissionsState.Loading)

    init {
        loadPermissions()
    }

    fun onAction(action: TvPermissionsAction) {
        when (action) {
            TvPermissionsAction.Retry -> loadPermissions()
            is TvPermissionsAction.SelectScope -> scope.value = action.scope
        }
    }

    private fun loadPermissions() {
        source.value = PermissionsSource.Loading
        viewModelScope.launch {
            source.value = withContext(dispatcherProvider.default()) {
                sectionRepository.detail(appDetailInput).fold(
                    onSuccess = { detail ->
                        PermissionsSource.Ready(
                            appName = detail.info.applicationName,
                            requested = permissionItemMapper.requested(detail),
                            defined = permissionItemMapper.defined(detail),
                        )
                    },
                    onFailure = { PermissionsSource.Error },
                )
            }
        }
    }
}

private sealed interface PermissionsSource {
    data object Loading : PermissionsSource
    data object Error : PermissionsSource
    data class Ready(
        val appName: String,
        val requested: List<PermissionItem>,
        val defined: List<PermissionItem>,
    ) : PermissionsSource
}

private val orderedProtectionLevels: List<ProtectionLevel?> = ProtectionLevel.entries + null

private fun PermissionsSource.Ready.toState(selectedScope: PermissionScope, initialPermissionName: String?): TvPermissionsState.Loaded {
    val scopeOptions = buildList {
        add(TvPermissionScopeOption(PermissionScope.Requested, requested.size))
        if (defined.isNotEmpty()) add(TvPermissionScopeOption(PermissionScope.Defined, defined.size))
    }
    val scope = selectedScope.takeIf { selected -> scopeOptions.any { it.scope == selected } } ?: PermissionScope.Requested
    val scoped = when (scope) {
        PermissionScope.Requested -> requested
        PermissionScope.Defined -> defined
    }
    return TvPermissionsState.Loaded(
        appName = appName,
        scope = scope,
        scopeOptions = scopeOptions,
        sections = orderedProtectionLevels.mapNotNull { level ->
            scoped.filter { it.protectionLevel == level }
                .takeIf { it.isNotEmpty() }
                ?.let { TvPermissionSection(level, it) }
        },
        initialPermissionName = initialPermissionName,
    )
}
