package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetailData
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class AppDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val appDetailRepository: AppDetailRepository, private val dispatcherProvider: DispatcherProvider) : ViewModel() {

    private val packageName: String? = savedStateHandle["packageName"]
    private val apkFilePath: String? = savedStateHandle["apkFilePath"]

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
            val result = withContext(dispatcherProvider.io()) {
                when {
                    packageName != null -> appDetailRepository.installedPackageDetails(packageName)
                    apkFilePath != null -> appDetailRepository.apkFilePackageDetails(File(apkFilePath))
                    else -> Result.failure(IllegalStateException("Either packageName or apkFilePath must be provided"))
                }
            }
            result.fold(
                onSuccess = { _state.value = it.toLoadedState() },
                onFailure = { _state.value = AppDetailState.Error(it.message ?: "Unknown error") },
            )
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
        usedPermissions = permissionData.usesPermissions
            .map { it.permissionData.simpleName }
            .toImmutableList(),
    )
}
