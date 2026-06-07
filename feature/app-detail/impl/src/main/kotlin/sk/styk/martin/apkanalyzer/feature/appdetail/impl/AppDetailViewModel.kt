package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetailData
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.io.File

private const val TAG = "AppDetailViewModel"

@HiltViewModel(assistedFactory = AppDetailViewModel.Factory::class)
internal class AppDetailViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): AppDetailViewModel
    }

    private val _state = MutableStateFlow<AppDetailState>(AppDetailState.Loading)
    val state: StateFlow<AppDetailState> = _state

    init {
        loadDetail()
    }

    fun onAction(action: AppDetailAction) {
        when (action) {
            is AppDetailAction.Retry -> loadDetail()
        }
    }

    private fun loadDetail() {
        _state.value = AppDetailState.Loading
        viewModelScope.launch {
            _state.value = withContext(dispatcherProvider.default()) {
                when (appDetailInput) {
                    is AppDetailInput.InstalledPackage -> appDetailRepository.installedPackageDetails(appDetailInput.packageName)
                    is AppDetailInput.ApkFile -> appDetailRepository.apkFilePackageDetails(File(appDetailInput.apkFilePath))
                }
            }.onFailure {
                Logger.e(TAG, it, "Can not load app detail")
            }.fold(
                onSuccess = { it.toLoadedState() },
                onFailure = { AppDetailState.Error },
            )
        }
    }
}

private fun AppDetailData.toLoadedState() = AppDetailState.Loaded(
    analysisMode = analysisMode,
    appName = generalData.applicationName,
    packageName = generalData.packageName,
    versionName = generalData.versionName,
    versionCode = generalData.versionCode,
    isSystemApp = generalData.isSystemApp,
    source = generalData.source.name,
    apkDirectory = generalData.apkDirectory,
    dataDirectory = generalData.dataDirectory,
    apkSize = generalData.apkSize,
    minSdkVersion = generalData.minSdkVersion,
    minSdkLabel = generalData.minSdkLabel,
    targetSdkVersion = generalData.targetSdkVersion,
    targetSdkLabel = generalData.targetSdkLabel,
    installLocation = generalData.installLocation.name,
    appInstaller = generalData.appInstaller,
    firstInstallTime = generalData.firstInstallTime,
    lastUpdateTime = generalData.lastUpdateTime,
    signAlgorithms = certificateData.map { it.signAlgorithm }.toImmutableList(),
    activitiesCount = activityData.size,
    servicesCount = serviceData.size,
    contentProvidersCount = contentProviderData.size,
    broadcastReceiversCount = broadcastReceiverData.size,
    definedPermissionsCount = permissionData.definesPermissions.size,
    usedPermissionsCount = permissionData.usesPermissions.size,
    featuresCount = featureData.size,
    usedPermissions = permissionData.usesPermissions.map { it.permissionData.simpleName }.toImmutableList(),
)
