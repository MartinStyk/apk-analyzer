package sk.styk.martin.apkanalyzer.feature.appexport.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.appexport.InventoryDestination
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportManager
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportProgress
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportRequest
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.format.INVENTORY_REFERENCE_URL
import sk.styk.martin.apkanalyzer.core.appexport.format.InventoryFormat
import sk.styk.martin.apkanalyzer.core.appexport.preferences.InventoryExportPreferences
import sk.styk.martin.apkanalyzer.core.appexport.preferences.InventorySelection
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterRepositories
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.feature.appexport.phone.apps.AppSelection
import sk.styk.martin.apkanalyzer.feature.appexport.phone.columns.ColumnSelection
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = InventoryExportViewModel.Factory::class)
internal class InventoryExportViewModel @AssistedInject constructor(
    @Assisted scope: InventoryExportScope,
    installedAppsRepository: InstalledAppsRepository,
    appFilterRepositories: AppFilterRepositories,
    private val inventoryExportManager: InventoryExportManager,
    private val inventoryExportPreferences: InventoryExportPreferences,
    usageStatsRepository: UsageStatsRepository,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(scope: InventoryExportScope): InventoryExportViewModel
    }

    private val steps = scope.steps
    private val apps = AppSelection(scope, installedAppsRepository, appFilterRepositories, dispatcherProvider, viewModelScope)
    private val columns = ColumnSelection()

    private val eventChannel = Channel<InventoryExportEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val step = MutableStateFlow(steps.first())
    private val format = MutableStateFlow(InventoryFormat.Csv)
    private val showStopConfirmation = MutableStateFlow(false)
    private val showIntro = MutableStateFlow(false)
    private val exportProgress = MutableStateFlow<InventoryExportProgressState>(InventoryExportProgressState.Idle)
    private var exportJob: Job? = null
    private var exitJob: Job? = null

    private val isExporting: Boolean
        get() = exportProgress.value is InventoryExportProgressState.Running

    private val previewContent: Flow<InventoryPreviewState> = combine(apps.selectedApps, columns.columns, format) { apps, columns, format ->
        apps?.takeIf { it.isNotEmpty() }?.let { inventoryRequest(it, columns, format) }?.takeIf { it.columns.isNotEmpty() }
    }
        .distinctUntilChanged()
        .transformLatest { request ->
            if (request == null) {
                emit(InventoryPreviewState.Unavailable)
            } else {
                emit(InventoryPreviewState.Loading)
                emit(
                    inventoryExportManager.preview(request).fold(
                        onSuccess = { InventoryPreviewState.Content(it.content, it.previewedAppCount, it.isTruncated) },
                        onFailure = { InventoryPreviewState.Unavailable },
                    ),
                )
            }
        }

    private val preview: Flow<InventoryPreviewState> = step
        .map { it == InventoryExportStep.Export }
        .distinctUntilChanged()
        .flatMapLatest { isVisible -> if (isVisible) previewContent else flowOf(InventoryPreviewState.Loading) }

    private val selection = combine(
        step,
        apps.state,
        columns.columns,
        usageStatsRepository.isPermissionGranted,
        format,
    ) { step, apps, columns, isUsageAccessGranted, format ->
        InventoryExportState(step = step, steps = steps, apps = apps, columns = columns, isUsageAccessGranted = isUsageAccessGranted, format = format)
    }

    val state: StateFlow<InventoryExportState> = combine(
        selection,
        preview,
        exportProgress,
        showStopConfirmation,
        showIntro,
    ) { selection, preview, progress, showStopConfirmation, showIntro ->
        selection.copy(preview = preview, progress = progress, showStopConfirmation = showStopConfirmation, showIntro = showIntro)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), InventoryExportState(step = steps.first(), steps = steps))

    private val restoreJob = viewModelScope.launch {
        showIntro.value = !inventoryExportPreferences.observeIntroDismissed().first()
        inventoryExportPreferences.observeLastSelection().first()?.let {
            columns.replace(it.columns)
            format.value = it.format
        }
    }

    fun onAction(action: InventoryExportAction) {
        if (exitJob?.isActive == true) return
        if (isExporting && !action.isExportControl) return
        when (action) {
            InventoryExportAction.Back -> goBack()

            InventoryExportAction.Next -> goNext()

            is InventoryExportAction.GoToStep -> if (action.step < step.value) step.value = action.step

            is InventoryExportAction.AppSelectionAction -> onAppSelectionAction(action)

            is InventoryExportAction.ColumnSelectionAction -> onColumnSelectionAction(action)

            InventoryExportAction.ShowIntro -> showIntro.value = true

            InventoryExportAction.DismissIntro -> {
                showIntro.value = false
                viewModelScope.launch { inventoryExportPreferences.dismissIntro() }
            }

            is InventoryExportAction.FormatSelected -> format.value = action.format

            InventoryExportAction.OpenFormatReference -> eventChannel.trySend(InventoryExportEvent.OpenFormatReference(INVENTORY_REFERENCE_URL))

            InventoryExportAction.Save -> if (state.value.canExport) {
                val format = format.value
                eventChannel.trySend(InventoryExportEvent.CreateDocument(format, format.fileName(LocalDateTime.now())))
            }

            is InventoryExportAction.SaveTo -> startExport(InventoryDestination.Document(action.destination), action.format)

            InventoryExportAction.Share -> startExport(InventoryDestination.Share, format.value)

            InventoryExportAction.DocumentPickerUnavailable -> showFeedback(InventoryExportFeedback.DocumentPickerUnavailable)

            InventoryExportAction.ShareUnavailable -> showFeedback(InventoryExportFeedback.ShareUnavailable)

            InventoryExportAction.CancelExport -> exportJob?.cancel()

            InventoryExportAction.DismissStopConfirmation -> showStopConfirmation.value = false

            InventoryExportAction.ConfirmStopAndBack -> if (showStopConfirmation.value) {
                showStopConfirmation.value = false
                exitJob = viewModelScope.launch {
                    exportJob?.cancelAndJoin()
                    eventChannel.send(InventoryExportEvent.NavigateBack)
                }
            }
        }
    }

    private fun onAppSelectionAction(action: InventoryExportAction.AppSelectionAction) {
        when (action) {
            is InventoryExportAction.SearchQueryChanged -> apps.search(action.query)
            is InventoryExportAction.AppToggled -> apps.toggle(action.packageName)
            InventoryExportAction.VisibleAppsToggled -> apps.toggleVisible()
            InventoryExportAction.EditFilters -> eventChannel.trySend(InventoryExportEvent.NavigateToFilter)
        }
    }

    private fun onColumnSelectionAction(action: InventoryExportAction.ColumnSelectionAction) {
        when (action) {
            is InventoryExportAction.PresetSelected -> columns.replace(action.preset.columns)
            is InventoryExportAction.ColumnToggled -> columns.toggle(action.column)
            is InventoryExportAction.ColumnGroupToggled -> columns.toggleGroup(action.group)
            InventoryExportAction.AllColumnsToggled -> columns.toggleAll()
            InventoryExportAction.OpenUsageAccessSettings -> eventChannel.trySend(InventoryExportEvent.OpenUsageAccessSettings)
        }
    }

    private fun goBack() {
        val previous = state.value.previousStep
        when {
            isExporting -> showStopConfirmation.value = true
            previous != null -> step.value = previous
            else -> eventChannel.trySend(InventoryExportEvent.NavigateBack)
        }
    }

    private fun goNext() {
        val next = state.value.nextStep ?: return
        if (state.value.canProceed) step.value = next
    }

    private fun startExport(destination: InventoryDestination, exportFormat: InventoryFormat) {
        if (isExporting) return
        exportProgress.value = InventoryExportProgressState.Running(processedApps = 0, totalApps = apps.selectedApps.value?.size ?: 0)
        exportJob = viewModelScope.launch {
            try {
                restoreJob.join()
                format.value = exportFormat
                val selection = InventorySelection(columns.columns.value, exportFormat)
                val request = inventoryRequest(apps.selectedApps.filterNotNull().first(), selection.columns, selection.format)
                if (request.apps.isEmpty() || request.columns.isEmpty()) {
                    showFeedback(InventoryExportFeedback.Failed)
                    return@launch
                }
                inventoryExportManager.export(request, destination).collect { update ->
                    when (update) {
                        is InventoryExportProgress.Running ->
                            exportProgress.value = InventoryExportProgressState.Running(update.processedApps, update.totalApps)

                        is InventoryExportProgress.Completed -> finishExport(update, destination, selection)

                        InventoryExportProgress.Failed -> showFeedback(InventoryExportFeedback.Failed)
                    }
                }
            } finally {
                exportProgress.value = InventoryExportProgressState.Idle
            }
        }
    }

    private suspend fun finishExport(
        completed: InventoryExportProgress.Completed,
        destination: InventoryDestination,
        selection: InventorySelection,
    ) {
        inventoryExportPreferences.saveSelection(selection)
        deliver(completed, destination, selection.format)
    }

    private fun deliver(
        completed: InventoryExportProgress.Completed,
        destination: InventoryDestination,
        format: InventoryFormat,
    ) {
        when (destination) {
            is InventoryDestination.Document -> showFeedback(
                InventoryExportFeedback.Saved(completed.displayName, completed.exportedAppCount, completed.incompleteAppCount),
            )

            InventoryDestination.Share -> {
                eventChannel.trySend(InventoryExportEvent.ShareFile(completed.uri, format.mimeType))
                if (completed.incompleteAppCount > 0) {
                    showFeedback(InventoryExportFeedback.SharedWithGaps(completed.incompleteAppCount))
                }
            }
        }
    }

    private fun showFeedback(feedback: InventoryExportFeedback) {
        eventChannel.trySend(InventoryExportEvent.ShowFeedback(feedback))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private val InventoryExportAction.isExportControl: Boolean
    get() = this is InventoryExportAction.Back ||
        this is InventoryExportAction.CancelExport ||
        this is InventoryExportAction.DismissStopConfirmation ||
        this is InventoryExportAction.ConfirmStopAndBack

private fun inventoryRequest(
    apps: List<InstalledApp>,
    columns: Set<InventoryColumn>,
    format: InventoryFormat,
) = InventoryExportRequest(
    apps = apps,
    columns = columns.exportedAs(format),
    format = format,
)
