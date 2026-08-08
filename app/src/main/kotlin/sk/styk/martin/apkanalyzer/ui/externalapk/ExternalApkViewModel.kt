package sk.styk.martin.apkanalyzer.ui.externalapk

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.apkfiles.TemporaryApkManager
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.feature.appdetail.api.ApkFileLifetime
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.io.File

@HiltViewModel(assistedFactory = ExternalApkViewModel.Factory::class)
internal class ExternalApkViewModel @AssistedInject constructor(
    @Assisted private val sourceUri: String,
    @Assisted private val taskId: Int,
    private val temporaryApkManager: TemporaryApkManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private var cachedFile = savedStateHandle
        .get<String>(CACHED_APK_PATH_KEY)
        ?.let(::File)
        ?.takeIf { it.isFile }

    val state: StateFlow<ExternalApkState>
        field = MutableStateFlow(cachedFile?.toLoadedState() ?: ExternalApkState.Loading)

    @AssistedFactory
    interface Factory {
        fun create(sourceUri: String, taskId: Int): ExternalApkViewModel
    }

    init {
        if (cachedFile == null) {
            savedStateHandle.remove<String>(CACHED_APK_PATH_KEY)
            load()
        }
    }

    override fun onCleared() {
        cachedFile?.let { file ->
            temporaryApkManager.release(file.absolutePath).onFailure { error ->
                Logger.e(TAG, error, "Unable to release external APK")
            }
        }
        savedStateHandle.remove<String>(CACHED_APK_PATH_KEY)
    }

    private fun load() {
        viewModelScope.launch {
            val result = temporaryApkManager.copy(
                uri = sourceUri.toUri(),
                taskId = taskId,
            )
            result.fold(
                onSuccess = { file ->
                    cachedFile?.let { previousFile ->
                        temporaryApkManager.release(previousFile.absolutePath).onFailure { error ->
                            Logger.e(TAG, error, "Unable to release replaced external APK")
                        }
                    }
                    cachedFile = file
                    savedStateHandle[CACHED_APK_PATH_KEY] = file.absolutePath
                    state.value = file.toLoadedState()
                },
                onFailure = { error ->
                    Logger.e(TAG, error, "Unable to copy external APK to cache")
                    state.value = ExternalApkState.Error
                },
            )
        }
    }

    private fun File.toLoadedState() = ExternalApkState.Loaded(
        AppDetailInput.ApkFile(
            apkFilePath = absolutePath,
            lifetime = ApkFileLifetime.Temporary,
        ),
    )
}

private const val TAG = "ExternalApkViewModel"
private const val CACHED_APK_PATH_KEY = "cached_apk_path"
