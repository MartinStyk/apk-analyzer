package sk.styk.martin.apkanalyzer.feature.appdetail.phone.generalinfo

import android.os.Build
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
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSection
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.isOnDevice
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.nativeLibrariesOrFallback
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.toSectionErrorReason

@HiltViewModel(assistedFactory = GeneralInfoViewModel.Factory::class)
internal class GeneralInfoViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val sectionRepository: AppDetailSectionRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(target: AppDetailInput): GeneralInfoViewModel
    }

    val state: StateFlow<GeneralInfoState>
        field = MutableStateFlow<GeneralInfoState>(GeneralInfoState.Loading)

    private val _events = Channel<GeneralInfoEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadDetail()
    }

    fun onAction(action: GeneralInfoAction) {
        when (action) {
            GeneralInfoAction.Retry -> loadDetail()

            is GeneralInfoAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { _events.send(GeneralInfoEvent.ShowCopiedFeedback) }
                }
            }
        }
    }

    private fun loadDetail() {
        state.value = GeneralInfoState.Loading
        viewModelScope.launch(dispatcherProvider.default()) {
            val deviceAbis = if (appDetailInput.isOnDevice) Build.SUPPORTED_ABIS.toList() else emptyList()
            sectionRepository.detail(appDetailInput)
                .onSuccess {
                    if (appDetailInput.isOnDevice) {
                        state.value = it.toGeneralInfoState(AppDetailSection.Available(NativeLibraries.Empty), deviceAbis)
                    }
                    state.value = it.toGeneralInfoState(sectionRepository.nativeLibrariesOrFallback(appDetailInput), deviceAbis)
                }
                .onFailure { failure ->
                    state.value = GeneralInfoState.Error(failure.toSectionErrorReason())
                }
        }
    }
}

private fun AppDetail.toGeneralInfoState(libraries: AppDetailSection<NativeLibraries>, deviceAbis: List<String>): GeneralInfoState.Loaded = toGeneralInfoState(
    libraries = (libraries as? AppDetailSection.Available)?.value ?: NativeLibraries.Empty,
    isNativeLibrariesRecorded = libraries is AppDetailSection.Available,
    deviceAbis = deviceAbis,
)

private fun AppDetail.toGeneralInfoState(
    libraries: NativeLibraries,
    isNativeLibrariesRecorded: Boolean,
    deviceAbis: List<String>,
) = GeneralInfoState.Loaded(
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
    nativeLibraryNames = libraries.libraryNames,
    deviceSupportedAbis = deviceAbis,
    isNativeLibraryDeviceIncompatible = deviceAbis.isNotEmpty() && libraries.hasNativeCode && libraries.abis.none { it in deviceAbis },
    isNativeLibrariesRecorded = isNativeLibrariesRecorded,
    installedSplitsCount = info.installedSplits.size,
)
