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
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySection
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraries
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.AppDetailDataSource
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.toRecordedVersionFailure

@HiltViewModel(assistedFactory = GeneralInfoViewModel.Factory::class)
internal class GeneralInfoViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val dataSource: AppDetailDataSource,
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
            val isHistorical = appDetailInput is AppDetailInput.Historical
            dataSource.detail(appDetailInput)
                .onSuccess {
                    if (!isHistorical) state.value = it.toGeneralInfoState(AppHistorySection.Recorded(NativeLibraries.Empty), isHistorical = false)
                    val nativeLibraries = dataSource.nativeLibraries(appDetailInput)
                    state.value = it.toGeneralInfoState(
                        libraries = if (isHistorical) {
                            nativeLibraries.getOrDefault(AppHistorySection.NotRecorded)
                        } else {
                            nativeLibraries.getOrDefault(AppHistorySection.Recorded(NativeLibraries.Empty))
                        },
                        isHistorical = isHistorical,
                    )
                }
                .onFailure { failure ->
                    state.value = failure.toRecordedVersionFailure()?.let { GeneralInfoState.RecordedVersionError(it) } ?: GeneralInfoState.Error
                }
        }
    }
}

private fun AppDetail.toGeneralInfoState(libraries: AppHistorySection<NativeLibraries>, isHistorical: Boolean): GeneralInfoState.Loaded {
    val recordedLibraries = (libraries as? AppHistorySection.Recorded)?.value ?: NativeLibraries.Empty
    return toGeneralInfoState(
        libraries = recordedLibraries,
        isNativeLibrariesRecorded = libraries is AppHistorySection.Recorded,
        deviceAbis = if (isHistorical) emptyList() else Build.SUPPORTED_ABIS.toList(),
    )
}

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
