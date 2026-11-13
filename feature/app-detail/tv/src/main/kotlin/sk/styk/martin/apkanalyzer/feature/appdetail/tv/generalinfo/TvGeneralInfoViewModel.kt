package sk.styk.martin.apkanalyzer.feature.appdetail.tv.generalinfo

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSection
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.nativeLibrariesOrFallback

@HiltViewModel(assistedFactory = TvGeneralInfoViewModel.Factory::class)
internal class TvGeneralInfoViewModel @AssistedInject constructor(
    @Assisted appDetailInput: AppDetailInput,
    sectionRepository: AppDetailSectionRepository,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appDetailInput: AppDetailInput): TvGeneralInfoViewModel
    }

    val state: StateFlow<TvGeneralInfoState>
        field = MutableStateFlow<TvGeneralInfoState>(TvGeneralInfoState.Loading)

    init {
        viewModelScope.launch(dispatcherProvider.default()) {
            val deviceAbis = Build.SUPPORTED_ABIS.toList()
            sectionRepository.detail(appDetailInput)
                .onSuccess { detail ->
                    state.value = detail.toGeneralInfoState(NativeLibraries.Empty, deviceAbis)
                    val section = sectionRepository.nativeLibrariesOrFallback(appDetailInput)
                    val nativeLibraries = (section as? AppDetailSection.Available)?.value ?: NativeLibraries.Empty
                    state.value = detail.toGeneralInfoState(nativeLibraries, deviceAbis)
                }
                .onFailure { state.value = TvGeneralInfoState.Error }
        }
    }
}

private fun AppDetail.toGeneralInfoState(libraries: NativeLibraries, deviceAbis: List<String>) = TvGeneralInfoState.Loaded(
    applicationName = info.applicationName,
    packageName = info.packageName,
    processName = info.processName,
    uid = info.uid,
    sharedUserId = info.sharedUserId,
    description = info.description,
    versionName = info.versionName,
    versionCode = info.versionCode,
    minSdkVersion = info.minSdkVersion,
    minSdkLabel = info.minSdkLabel,
    targetSdkVersion = info.targetSdkVersion,
    targetSdkLabel = info.targetSdkLabel,
    isSystemApp = info.isSystemApp,
    isDebuggable = info.isDebuggable,
    allowsBackup = info.allowsBackup,
    usesCleartextTraffic = info.usesCleartextTraffic,
    source = info.source,
    installSourceChain = info.installSourceChain,
    firstInstallTime = info.firstInstallTime,
    lastUpdateTime = info.lastUpdateTime,
    lastUsedTime = info.lastUsedTime,
    apkDirectory = info.apkDirectory,
    dataDirectory = info.dataDirectory,
    installLocation = info.installLocation.name,
    apkSize = info.apkSize,
    totalSize = info.totalSize,
    nativeLibraryAbis = libraries.abis,
    nativeLibraryCount = libraries.libraryNames.size,
    deviceSupportedAbis = deviceAbis,
    isNativeLibraryDeviceIncompatible = deviceAbis.isNotEmpty() && libraries.hasNativeCode && libraries.abis.none { it in deviceAbis },
    installedSplitsCount = info.installedSplits.size,
)
