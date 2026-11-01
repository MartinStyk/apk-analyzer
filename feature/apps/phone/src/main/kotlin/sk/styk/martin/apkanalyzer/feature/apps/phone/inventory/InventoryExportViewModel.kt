package sk.styk.martin.apkanalyzer.feature.apps.phone.inventory

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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportManager
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportProgress
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportRequest
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterRepository
import sk.styk.martin.apkanalyzer.core.apps.filtering.InventoryExportFilter
import sk.styk.martin.apkanalyzer.core.apps.filtering.filterBy
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.apps.sorting.comparator
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.file.ShareFileManager
import sk.styk.martin.apkanalyzer.feature.apps.phone.list.AppListState
import sk.styk.martin.apkanalyzer.feature.apps.phone.list.toListItem
import java.time.LocalDateTime
import javax.inject.Inject

private const val SHARE_DIRECTORY = "inventory"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryExportViewModel @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    listFilterRepository: AppFilterRepository,
    @InventoryExportFilter private val exportFilterRepository: AppFilterRepository,
    usageStatsRepository: UsageStatsRepository,
    private val inventoryExportManager: InventoryExportManager,
    private val shareFileManager: ShareFileManager,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val form = MutableStateFlow(InventoryExportForm())
    private val progress = MutableStateFlow<InventoryExportProgressState>(InventoryExportProgressState.Idle)
    private var exportJob: Job? = null

    private val eventChannel = Channel<InventoryExportEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        exportFilterRepository.update(listFilterRepository.filter.value)
    }

    private val selectedApps: StateFlow<List<InstalledApp>?> = combine(
        installedAppsRepository.apps(),
        exportFilterRepository.filter,
    ) { apps, filter ->
        apps.filterBy(filter).sortedWith(AppSortType.Name.comparator(ascending = true))
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val appListState = selectedApps
        .map { apps -> apps?.let { AppListState.Content(it.map { app -> app.toListItem() }) } ?: AppListState.Loading }
        .flowOn(dispatcherProvider.default())

    private val preview = combine(selectedApps, form) { apps, form ->
        apps?.takeIf { form.step == InventoryExportStep.Export && form.selectedAttributes.isNotEmpty() }
            ?.let { form.toRequest(it) }
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

    val state: StateFlow<InventoryExportState> = combine(
        appListState,
        exportFilterRepository.filter,
        form,
        usageStatsRepository.isPermissionGranted,
        combine(preview, progress, ::Pair),
    ) { apps, filter, form, isUsageAccessGranted, (preview, progress) ->
        InventoryExportState(
            step = form.step,
            apps = apps,
            isFilterActive = filter.isActive,
            selectedAttributes = form.selectedAttributes,
            isUsageAccessGranted = isUsageAccessGranted,
            format = form.format,
            preview = preview,
            progress = progress,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryExportState())

    fun onAction(action: InventoryExportAction) {
        when (action) {
            InventoryExportAction.NavigateBack -> navigateBack()

            InventoryExportAction.Continue -> if (state.value.canContinue) {
                form.update { it.copy(step = it.step.next()) }
            }

            is InventoryExportAction.StepSelected -> if (action.step < form.value.step && !isExporting()) {
                form.update { it.copy(step = action.step) }
            }

            InventoryExportAction.EditFilters -> eventChannel.trySend(InventoryExportEvent.NavigateToFilter)

            InventoryExportAction.IncludeAllApps -> exportFilterRepository.clear()

            is InventoryExportAction.AppClicked -> eventChannel.trySend(InventoryExportEvent.NavigateToAppDetail(action.packageName))

            is InventoryExportAction.AttributeToggled -> form.update {
                it.copy(selectedAttributes = it.selectedAttributes.toggle(action.attribute))
            }

            is InventoryExportAction.GroupToggled -> form.update {
                it.copy(selectedAttributes = it.selectedAttributes.toggleAll(action.group.attributes()))
            }

            InventoryExportAction.AllAttributesToggled -> form.update {
                it.copy(selectedAttributes = it.selectedAttributes.toggleAll(InventoryAttribute.entries))
            }

            InventoryExportAction.OpenUsageAccessSettings -> eventChannel.trySend(InventoryExportEvent.OpenUsageAccessSettings)

            is InventoryExportAction.FormatSelected -> if (!isExporting()) {
                form.update { it.copy(format = action.format) }
            }

            InventoryExportAction.Save -> if (!isExporting()) {
                val format = form.value.format
                eventChannel.trySend(InventoryExportEvent.CreateDocument(format, format.fileName(LocalDateTime.now())))
            }

            is InventoryExportAction.SaveTo -> startExport(ExportTarget.File, action.format) { Result.success(action.destination) }

            InventoryExportAction.DocumentPickerUnavailable -> showFeedback(InventoryExportFeedback.DocumentPickerUnavailable)

            InventoryExportAction.Share -> {
                val format = form.value.format
                val fileName = format.fileName(LocalDateTime.now())
                startExport(ExportTarget.Share, format) { shareFileManager.createShareFile(directory = SHARE_DIRECTORY, fileName = fileName) }
            }

            InventoryExportAction.ShareUnavailable -> showFeedback(InventoryExportFeedback.ShareUnavailable)

            InventoryExportAction.CancelExport -> exportJob?.cancel()
        }
    }

    private fun navigateBack() {
        val step = form.value.step
        when {
            isExporting() -> exportJob?.cancel()
            step != InventoryExportStep.Apps -> form.update { it.copy(step = step.previous()) }
            else -> eventChannel.trySend(InventoryExportEvent.NavigateBack)
        }
    }

    private fun isExporting() = exportJob?.isActive == true

    private fun startExport(
        target: ExportTarget,
        format: InventoryFormat,
        destination: suspend () -> Result<Uri>,
    ) {
        val apps = selectedApps.value
        if (isExporting() || apps.isNullOrEmpty() || form.value.selectedAttributes.isEmpty()) return
        val request = form.value.copy(format = format).toRequest(apps)
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

                        is InventoryExportProgress.Completed -> onExportCompleted(update, target, uri, request.format)

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
                eventChannel.trySend(InventoryExportEvent.ShareFile(uri, format.mimeType))
                if (completed.incompleteAppCount > 0) {
                    showFeedback(InventoryExportFeedback.SharedWithGaps(completed.incompleteAppCount))
                }
            }
        }
    }

    private fun showFeedback(feedback: InventoryExportFeedback) {
        eventChannel.trySend(InventoryExportEvent.ShowFeedback(feedback))
    }

    private enum class ExportTarget {
        File,
        Share,
    }
}

private data class InventoryExportForm(
    val step: InventoryExportStep = InventoryExportStep.Apps,
    val selectedAttributes: Set<InventoryAttribute> = InventoryAttribute.Defaults,
    val format: InventoryFormat = InventoryFormat.Csv,
) {
    fun toRequest(apps: List<InstalledApp>) = InventoryExportRequest(
        apps = apps,
        attributes = InventoryAttribute.entries.filter { it in selectedAttributes },
        format = format,
    )
}

private fun InventoryExportStep.next() = InventoryExportStep.entries.getOrElse(ordinal + 1) { this }

private fun InventoryExportStep.previous() = InventoryExportStep.entries.getOrElse(ordinal - 1) { this }

private fun Set<InventoryAttribute>.toggle(attribute: InventoryAttribute) = if (attribute in this) this - attribute else this + attribute

private fun Set<InventoryAttribute>.toggleAll(attributes: List<InventoryAttribute>) =
    if (containsAll(attributes)) this - attributes.toSet() else this + attributes
