package sk.styk.martin.apkanalyzer.ui.externalapk

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.util.FileUtil
import sk.styk.martin.apkanalyzer.feature.appdetail.api.ApkFileLifetime
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.io.File

@HiltViewModel(assistedFactory = ExternalApkViewModel.Factory::class)
internal class ExternalApkViewModel @AssistedInject constructor(
    @Assisted private val sourceUri: String,
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
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
        fun create(sourceUri: String): ExternalApkViewModel
    }

    init {
        if (cachedFile == null) {
            savedStateHandle.remove<String>(CACHED_APK_PATH_KEY)
            load()
        }
    }

    override fun onCleared() {
        cachedFile?.delete()
        savedStateHandle.remove<String>(CACHED_APK_PATH_KEY)
    }

    private fun load() {
        viewModelScope.launch {
            val result = FileUtil.copyApkUriToCache(
                context = context,
                uri = sourceUri.toUri(),
                dispatcherProvider = dispatcherProvider,
            )
            result.fold(
                onSuccess = { file ->
                    cachedFile?.delete()
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
