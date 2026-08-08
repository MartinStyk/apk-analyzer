package sk.styk.martin.apkanalyzer.feature.apps.impl.components.apkfilepicker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.util.FileUtil
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class ApkFilePickerViewModel @Inject constructor(@ApplicationContext private val context: Context, private val dispatcherProvider: DispatcherProvider) : ViewModel() {

    val state: StateFlow<ApkFilePickerState>
        field = MutableStateFlow<ApkFilePickerState>(ApkFilePickerState.Ready)

    private val eventChannel = Channel<ApkFilePickerEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var cachedApkFile: File? = null

    fun onAction(action: ApkFilePickerAction) {
        when (action) {
            ApkFilePickerAction.OpenPicker -> eventChannel.trySend(ApkFilePickerEvent.OpenDocument)

            is ApkFilePickerAction.ApkSelected -> copyApkToCache(action.uri)

            is ApkFilePickerAction.ApkDetailOpened -> {
                if (cachedApkFile?.absolutePath == action.apkFilePath) {
                    cachedApkFile = null
                }
            }
        }
    }

    override fun onCleared() {
        cachedApkFile?.delete()
    }

    private fun copyApkToCache(uri: android.net.Uri) {
        if (state.value == ApkFilePickerState.Copying) return
        state.value = ApkFilePickerState.Copying
        viewModelScope.launch {
            FileUtil.copyApkUriToCache(
                context = context,
                uri = uri,
                dispatcherProvider = dispatcherProvider,
            ).fold(
                onSuccess = { file ->
                    cachedApkFile?.delete()
                    cachedApkFile = file
                    state.value = ApkFilePickerState.Ready
                    eventChannel.send(ApkFilePickerEvent.OpenApkDetail(file.absolutePath))
                },
                onFailure = { error ->
                    Logger.e(TAG, error, "Unable to copy selected APK to cache")
                    state.value = ApkFilePickerState.Ready
                    eventChannel.send(ApkFilePickerEvent.ShowOpenError)
                },
            )
        }
    }
}

private const val TAG = "ApkFilePickerViewModel"
