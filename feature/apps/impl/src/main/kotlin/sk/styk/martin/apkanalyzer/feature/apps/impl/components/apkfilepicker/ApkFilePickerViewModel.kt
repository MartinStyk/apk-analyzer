package sk.styk.martin.apkanalyzer.feature.apps.impl.components.apkfilepicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apkfiles.ApkTooLargeException
import sk.styk.martin.apkanalyzer.core.apkfiles.TemporaryApkManager
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class ApkFilePickerViewModel @Inject constructor(private val temporaryApkManager: TemporaryApkManager) : ViewModel() {

    val state: StateFlow<ApkFilePickerState>
        field = MutableStateFlow<ApkFilePickerState>(ApkFilePickerState.Ready)

    private val eventChannel = Channel<ApkFilePickerEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var cachedApkFile: File? = null

    fun onAction(action: ApkFilePickerAction) {
        when (action) {
            ApkFilePickerAction.OpenPicker -> eventChannel.trySend(ApkFilePickerEvent.OpenDocument)

            is ApkFilePickerAction.ApkSelected -> copyApkToCache(action.uri, action.taskId)

            is ApkFilePickerAction.ApkDetailOpened -> {
                if (cachedApkFile?.absolutePath == action.apkFilePath) {
                    cachedApkFile = null
                }
            }
        }
    }

    override fun onCleared() {
        cachedApkFile?.let { file ->
            temporaryApkManager.release(file.absolutePath).onFailure { error ->
                Logger.e(TAG, error, "Unable to release selected APK")
            }
        }
    }

    private fun copyApkToCache(uri: android.net.Uri, taskId: Int) {
        if (state.value == ApkFilePickerState.Copying) return
        state.value = ApkFilePickerState.Copying
        viewModelScope.launch {
            temporaryApkManager.copy(
                uri = uri,
                taskId = taskId,
            ).fold(
                onSuccess = { file ->
                    cachedApkFile?.let { previousFile ->
                        temporaryApkManager.release(previousFile.absolutePath).onFailure { error ->
                            Logger.e(TAG, error, "Unable to release replaced selected APK")
                        }
                    }
                    cachedApkFile = file
                    state.value = ApkFilePickerState.Ready
                    eventChannel.send(ApkFilePickerEvent.OpenApkDetail(file.absolutePath))
                },
                onFailure = { error ->
                    if (error is ApkTooLargeException) {
                        Logger.w(TAG, error.message.orEmpty())
                    } else {
                        Logger.e(TAG, error, "Unable to copy selected APK to cache")
                    }
                    state.value = ApkFilePickerState.Ready
                    eventChannel.send(ApkFilePickerEvent.ShowOpenError)
                },
            )
        }
    }
}

private const val TAG = "ApkFilePickerViewModel"
