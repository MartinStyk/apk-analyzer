package sk.styk.martin.apkanalyzer.feature.settings.phone.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apphistory.api.capture.AppHistoryRecordingRepository
import sk.styk.martin.apkanalyzer.core.apphistory.api.storage.AppHistoryStorageRepository
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
internal class AppHistorySettingsViewModel @Inject constructor(
    private val recordingRepository: AppHistoryRecordingRepository,
    private val storageRepository: AppHistoryStorageRepository,
) : ViewModel() {

    private val eventChannel = Channel<AppHistorySettingsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val transfer = MutableStateFlow(AppHistoryTransfer.Idle)
    private val isTurnOffConfirmationVisible = MutableStateFlow(false)

    val state: StateFlow<AppHistorySettingsState> = combine(
        recordingRepository.recording(),
        storageRepository.usage(),
        transfer,
        isTurnOffConfirmationVisible,
    ) { recording, usage, transfer, isTurnOffConfirmationVisible ->
        AppHistorySettingsState.Loaded(
            recording = recording,
            usage = usage,
            transfer = transfer,
            isTurnOffConfirmationVisible = isTurnOffConfirmationVisible,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppHistorySettingsState.Loading)

    fun onAction(action: AppHistorySettingsAction) {
        when (action) {
            is AppHistorySettingsAction.SetRecording -> {
                if (action.enabled) saveRecording(enabled = true) else isTurnOffConfirmationVisible.value = true
            }

            AppHistorySettingsAction.ConfirmTurnOff -> {
                isTurnOffConfirmationVisible.value = false
                saveRecording(enabled = false)
            }

            AppHistorySettingsAction.DismissTurnOff -> isTurnOffConfirmationVisible.value = false

            AppHistorySettingsAction.Export -> {
                if (transfer.value == AppHistoryTransfer.Idle) sendEvent(AppHistorySettingsEvent.CreateExportDocument(exportFileName()))
            }

            is AppHistorySettingsAction.ExportTo -> export(action.destination)

            AppHistorySettingsAction.Import -> {
                if (transfer.value == AppHistoryTransfer.Idle) sendEvent(AppHistorySettingsEvent.OpenImportDocument)
            }

            is AppHistorySettingsAction.ImportFrom -> import(action.source)

            AppHistorySettingsAction.DocumentPickerUnavailable -> {
                sendEvent(AppHistorySettingsEvent.ShowFeedback(AppHistorySettingsFeedback.DocumentPickerUnavailable))
            }

            AppHistorySettingsAction.NavigateBack -> sendEvent(AppHistorySettingsEvent.NavigateBack)
        }
    }

    private fun saveRecording(enabled: Boolean) {
        viewModelScope.launch { recordingRepository.setRecordingEnabled(enabled) }
    }

    private fun export(destination: Uri) {
        if (!transfer.compareAndSet(AppHistoryTransfer.Idle, AppHistoryTransfer.Exporting)) return
        viewModelScope.launch {
            val feedback = storageRepository.export(destination).fold(
                onSuccess = { AppHistorySettingsFeedback.Exported },
                onFailure = { AppHistorySettingsFeedback.ExportFailed },
            )
            transfer.value = AppHistoryTransfer.Idle
            eventChannel.send(AppHistorySettingsEvent.ShowFeedback(feedback))
        }
    }

    private fun import(source: Uri) {
        if (!transfer.compareAndSet(AppHistoryTransfer.Idle, AppHistoryTransfer.Importing)) return
        viewModelScope.launch {
            val feedback = storageRepository.import(source).fold(
                onSuccess = { imported ->
                    if (imported.importedAppCount == 0) {
                        AppHistorySettingsFeedback.NothingNewToImport
                    } else {
                        AppHistorySettingsFeedback.Imported(imported.importedAppCount)
                    }
                },
                onFailure = { AppHistorySettingsFeedback.ImportFailed },
            )
            transfer.value = AppHistoryTransfer.Idle
            eventChannel.send(AppHistorySettingsEvent.ShowFeedback(feedback))
        }
    }

    private fun sendEvent(event: AppHistorySettingsEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun exportFileName(): String = "apk-analyzer-history-${LocalDate.now()}.db"
}
