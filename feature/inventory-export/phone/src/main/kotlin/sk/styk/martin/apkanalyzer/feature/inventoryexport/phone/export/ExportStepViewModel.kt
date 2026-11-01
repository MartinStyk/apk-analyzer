package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportManager
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportProgress
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportRequest
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.file.ShareFileManager
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.draft.InventoryExportDraft
import java.time.LocalDateTime
import javax.inject.Inject

private const val SHARE_DIRECTORY = "inventory"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExportStepViewModel @Inject constructor(
    private val draft: InventoryExportDraft,
    private val inventoryExportManager: InventoryExportManager,
    private val shareFileManager: ShareFileManager,
) : ViewModel() {

    private val progress = MutableStateFlow<InventoryExportProgressState>(InventoryExportProgressState.Idle)
    private var exportJob: Job? = null

    private val eventChannel = Channel<ExportStepEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val selectedApps: StateFlow<List<InstalledApp>?> = draft.apps
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val preview = combine(selectedApps, draft.attributes, draft.format) { apps, attributes, format ->
        apps?.takeIf { attributes.isNotEmpty() }?.let { inventoryRequest(it, attributes, format) }
    }
        .distinctUntilChanged()
        .transformLatest { request ->
            emit(InventoryPreviewState.Loading)
            if (request != null) {
                val preview = inventoryExportManager.preview(request).fold(
                    onSuccess = { InventoryPreviewState.Content(it.content, it.previewedAppCount, it.isTruncated) },
                    onFailure = { InventoryPreviewState.Unavailable },
                )
                emit(preview)
            }
        }

    val state: StateFlow<ExportStepState> = combine(
        selectedApps,
        draft.attributes,
        draft.format,
        preview,
        progress,
    ) { apps, attributes, format, preview, progress ->
        ExportStepState(
            appCount = apps?.size ?: 0,
            selectedAttributes = attributes,
            format = format,
            preview = preview,
            progress = progress,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ExportStepState(selectedAttributes = draft.attributes.value, format = draft.format.value),
    )

    init {
        if (!draft.isStarted) eventChannel.trySend(ExportStepEvent.NavigateToStep(InventoryExportStep.Apps))
    }

    fun onAction(action: ExportStepAction) {
        when (action) {
            ExportStepAction.NavigateBack -> if (isExporting()) {
                exportJob?.cancel()
            } else {
                eventChannel.trySend(ExportStepEvent.NavigateBack)
            }

            is ExportStepAction.StepSelected -> if (!isExporting()) {
                eventChannel.trySend(ExportStepEvent.NavigateToStep(action.step))
            }

            is ExportStepAction.FormatSelected -> if (!isExporting()) {
                draft.selectFormat(action.format)
            }

            ExportStepAction.Save -> if (!isExporting()) {
                val format = draft.format.value
                eventChannel.trySend(ExportStepEvent.CreateDocument(format, format.fileName(LocalDateTime.now())))
            }

            is ExportStepAction.SaveTo -> startExport(ExportTarget.File, action.format) { Result.success(action.destination) }

            ExportStepAction.DocumentPickerUnavailable -> showFeedback(InventoryExportFeedback.DocumentPickerUnavailable)

            ExportStepAction.Share -> {
                val format = draft.format.value
                val fileName = format.fileName(LocalDateTime.now())
                startExport(ExportTarget.Share, format) { shareFileManager.createShareFile(directory = SHARE_DIRECTORY, fileName = fileName) }
            }

            ExportStepAction.ShareUnavailable -> showFeedback(InventoryExportFeedback.ShareUnavailable)

            ExportStepAction.CancelExport -> exportJob?.cancel()
        }
    }

    private fun isExporting() = exportJob?.isActive == true

    private fun startExport(
        target: ExportTarget,
        format: InventoryFormat,
        destination: suspend () -> Result<Uri>,
    ) {
        val apps = selectedApps.value
        val attributes = draft.attributes.value
        if (isExporting() || apps.isNullOrEmpty() || attributes.isEmpty()) return
        val request = inventoryRequest(apps, attributes, format)
        exportJob = viewModelScope.launch {
            progress.value = InventoryExportProgressState.Running(processedApps = 0, totalApps = apps.size)
            try {
                val uri = destination().getOrElse {
                    showFeedback(InventoryExportFeedback.Failed)
                    return@launch
                }
                inventoryExportManager.export(request, uri).collect { update ->
                    when (update) {
                        is InventoryExportProgress.Running ->
                            progress.value =
                                InventoryExportProgressState.Running(update.processedApps, update.totalApps)

                        is InventoryExportProgress.Completed -> onExportCompleted(update, target, uri, format)

                        InventoryExportProgress.Failed -> showFeedback(InventoryExportFeedback.Failed)
                    }
                }
            } finally {
                progress.value = InventoryExportProgressState.Idle
            }
        }
    }

    private fun onExportCompleted(
        completed: InventoryExportProgress.Completed,
        target: ExportTarget,
        uri: Uri,
        format: InventoryFormat,
    ) {
        when (target) {
            ExportTarget.File -> showFeedback(
                InventoryExportFeedback.Saved(
                    displayName = completed.displayName,
                    appCount = completed.exportedAppCount,
                    incompleteAppCount = completed.incompleteAppCount,
                ),
            )

            ExportTarget.Share -> {
                eventChannel.trySend(ExportStepEvent.ShareFile(uri, format.mimeType))
                if (completed.incompleteAppCount > 0) {
                    showFeedback(InventoryExportFeedback.SharedWithGaps(completed.incompleteAppCount))
                }
            }
        }
    }

    private fun showFeedback(feedback: InventoryExportFeedback) {
        eventChannel.trySend(ExportStepEvent.ShowFeedback(feedback))
    }

    private enum class ExportTarget {
        File,
        Share,
    }
}

private fun inventoryRequest(
    apps: List<InstalledApp>,
    attributes: Set<InventoryAttribute>,
    format: InventoryFormat,
) = InventoryExportRequest(
    apps = apps,
    attributes = InventoryAttribute.entries.filter { it in attributes },
    format = format,
)
