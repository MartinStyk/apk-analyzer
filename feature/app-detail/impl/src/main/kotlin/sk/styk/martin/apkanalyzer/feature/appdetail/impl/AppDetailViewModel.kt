package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import android.content.pm.PermissionInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.io.File

private const val TAG = "AppDetailViewModel"

@HiltViewModel(assistedFactory = AppDetailViewModel.Factory::class)
internal class AppDetailViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
    private val storageStatsRepository: StorageStatsRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): AppDetailViewModel
    }

    private val _state = MutableStateFlow<AppDetailState>(AppDetailState.Loading)
    val state: StateFlow<AppDetailState> = _state

    private val eventChannel = Channel<AppDetailEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadDetail()
    }

    fun onAction(action: AppDetailAction) {
        when (action) {
            is AppDetailAction.Retry -> loadDetail()
            is AppDetailAction.ViewManifest -> sendEvent(AppDetailEvent.NavigateToManifest)
            is AppDetailAction.ExportApk -> withLoadedState { sendEvent(AppDetailEvent.ExportApk(it.packageName)) }
            is AppDetailAction.SaveIcon -> withLoadedState { sendEvent(AppDetailEvent.SaveIcon(it.packageName)) }
            is AppDetailAction.OpenPlayStore -> withLoadedState { sendEvent(AppDetailEvent.OpenPlayStore(it.packageName)) }
            is AppDetailAction.OpenAppInfo -> withLoadedState { sendEvent(AppDetailEvent.OpenAppInfo(it.packageName)) }
            is AppDetailAction.NavigateGeneralDetails -> sendEvent(AppDetailEvent.NavigateToGeneralDetails)
            is AppDetailAction.NavigatePermissions -> sendEvent(AppDetailEvent.NavigateToPermissions)
            is AppDetailAction.NavigateActivities -> sendEvent(AppDetailEvent.NavigateToActivities)
            is AppDetailAction.NavigateServices -> sendEvent(AppDetailEvent.NavigateToServices)
            is AppDetailAction.NavigateReceivers -> sendEvent(AppDetailEvent.NavigateToReceivers)
            is AppDetailAction.NavigateProviders -> sendEvent(AppDetailEvent.NavigateToProviders)
            is AppDetailAction.NavigateCertificates -> sendEvent(AppDetailEvent.NavigateToCertificates)
            is AppDetailAction.NavigateFeatures -> sendEvent(AppDetailEvent.NavigateToFeatures)
        }
    }

    private fun sendEvent(event: AppDetailEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun withLoadedState(block: (AppDetailState.Loaded) -> Unit) {
        (_state.value as? AppDetailState.Loaded)?.let(block)
    }

    private fun loadDetail() {
        _state.value = AppDetailState.Loading
        viewModelScope.launch {
            val loaded = withContext(dispatcherProvider.default()) {
                when (appDetailInput) {
                    is AppDetailInput.InstalledPackage -> appDetailRepository.installedPackageDetails(appDetailInput.packageName)
                    is AppDetailInput.ApkFile -> appDetailRepository.apkFilePackageDetails(File(appDetailInput.apkFilePath))
                }
            }.onFailure {
                Logger.e(TAG, it, "Can not load app detail for $appDetailInput")
            }.fold(
                onSuccess = { it.toLoadedState() },
                onFailure = { AppDetailState.Error },
            )
            _state.value = loaded

            if (loaded is AppDetailState.Loaded && appDetailInput is AppDetailInput.InstalledPackage) {
                storageStatsRepository.requestTotalSizes(listOf(appDetailInput.packageName))
                storageStatsRepository.totalSizes.collect { dataResult ->
                    val totalSize = (dataResult as? StorageStatsRepository.DataResult.Available)
                        ?.data?.get(appDetailInput.packageName)?.bytes
                    _state.update { current ->
                        (current as? AppDetailState.Loaded)?.copy(totalSize = totalSize) ?: current
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun AppDetail.toLoadedState() = AppDetailState.Loaded(
    analysisMode = analysisMode,
    appName = info.applicationName,
    packageName = info.packageName,
    processName = info.processName,
    versionName = info.versionName,
    versionCode = info.versionCode,
    uid = info.uid,
    description = info.description,
    isSystemApp = info.isSystemApp,
    source = info.source.name,
    apkDirectory = info.apkDirectory,
    dataDirectory = info.dataDirectory,
    apkSize = info.apkSize,
    targetSdkVersion = info.targetSdkVersion,
    targetSdkLabel = info.targetSdkLabel,
    minSdkVersion = info.minSdkVersion,
    minSdkLabel = info.minSdkLabel,
    installLocation = info.installLocation.name,
    appInstaller = info.appInstaller,
    firstInstallTime = info.firstInstallTime,
    lastUpdateTime = info.lastUpdateTime,
    totalPermissionsCount = permissions.used.size,
    dangerousPermissionsCount = permissions.used.count {
        it.permissionData.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE == PermissionInfo.PROTECTION_DANGEROUS
    },
    definedPermissionsCount = permissions.defined.size,
    activitiesCount = activities.size,
    servicesCount = services.size,
    contentProvidersCount = contentProviders.size,
    broadcastReceiversCount = receivers.size,
    certificatesCount = certificates.size,
    featuresCount = features.size,
)
