package sk.styk.martin.apkanalyzer.feature.appdetail.impl.generalinfo

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.clipboard.ClipboardManager
import sk.styk.martin.apkanalyzer.core.common.clipboard.CopyResult
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.toAppReference

@HiltViewModel(assistedFactory = GeneralInfoViewModel.Factory::class)
internal class GeneralInfoViewModel @AssistedInject constructor(
    @Assisted private val appDetailInput: AppDetailInput,
    private val appDetailRepository: AppDetailRepository,
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
            is GeneralInfoAction.CopyValue -> {
                if (clipboardManager.copy(action.label, action.value) == CopyResult.FeedbackNotShown) {
                    viewModelScope.launch { _events.send(GeneralInfoEvent.ShowCopiedFeedback) }
                }
            }
        }
    }

    private fun loadDetail() {
        state.value = GeneralInfoState.Loading
        viewModelScope.launch {
            state.value = withContext(dispatcherProvider.default()) {
                appDetailRepository.details(appDetailInput.toAppReference())
            }.fold(
                onSuccess = { it.toGeneralInfoState() },
                onFailure = { GeneralInfoState.Error },
            )
        }
    }
}

private fun AppDetail.toGeneralInfoState() = GeneralInfoState.Loaded(
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
    nativeLibraryAbis = nativeLibraries.abis.toImmutableList(),
    nativeLibraryNames = nativeLibraries.libraryNames.toImmutableList(),
    deviceSupportedAbis = Build.SUPPORTED_ABIS.toList().toImmutableList(),
    isNativeLibraryDeviceIncompatible = nativeLibraries.hasNativeCode &&
        nativeLibraries.abis.none { it in Build.SUPPORTED_ABIS },
    installedSplitsCount = info.installedSplits.size,
)
