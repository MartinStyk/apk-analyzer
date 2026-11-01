package sk.styk.martin.apkanalyzer.feature.apps.phone.inventory

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.OutlinedButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ProgressBar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.launcher.launchSafely
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.apps.phone.R
import sk.styk.martin.apkanalyzer.feature.apps.phone.list.AppListState

@Composable
internal fun InventoryExportScreen(
    onBack: () -> Unit,
    onEditFilters: () -> Unit,
    onAppDetails: (PackageName) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryExportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val csvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(InventoryFormat.Csv.mimeType),
    ) { destination ->
        if (destination != null) viewModel.onAction(InventoryExportAction.SaveTo(destination, InventoryFormat.Csv))
    }
    val jsonDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(InventoryFormat.Json.mimeType),
    ) { destination ->
        if (destination != null) viewModel.onAction(InventoryExportAction.SaveTo(destination, InventoryFormat.Json))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                InventoryExportEvent.NavigateBack -> onBack()

                InventoryExportEvent.NavigateToFilter -> onEditFilters()

                is InventoryExportEvent.NavigateToAppDetail -> onAppDetails(event.packageName)

                InventoryExportEvent.OpenUsageAccessSettings -> context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

                is InventoryExportEvent.CreateDocument -> {
                    val launcher = when (event.format) {
                        InventoryFormat.Csv -> csvDocumentLauncher
                        InventoryFormat.Json -> jsonDocumentLauncher
                    }
                    launcher.launchSafely(event.fileName) {
                        viewModel.onAction(InventoryExportAction.DocumentPickerUnavailable)
                    }
                }

                is InventoryExportEvent.ShareFile -> try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        clipData = ClipData.newRawUri(null, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                } catch (_: ActivityNotFoundException) {
                    viewModel.onAction(InventoryExportAction.ShareUnavailable)
                }

                is InventoryExportEvent.ShowFeedback ->
                    Toast.makeText(context, event.feedback.message(context), Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler {
        viewModel.onAction(InventoryExportAction.NavigateBack)
    }

    InventoryExportContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun InventoryExportContent(
    state: InventoryExportState,
    onAction: (InventoryExportAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.inventory_export_title),
            onBack = { onAction(InventoryExportAction.NavigateBack) },
        )

        InventoryStepIndicator(
            currentStep = state.step,
            canReturn = !state.isExporting,
            onStepSelect = { onAction(InventoryExportAction.StepSelected(it)) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "inventoryExportStep",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { step ->
            when (step) {
                InventoryExportStep.Apps -> InventoryAppsStep(
                    apps = state.apps,
                    isFilterActive = state.isFilterActive,
                    onEditFilters = { onAction(InventoryExportAction.EditFilters) },
                    onIncludeAllApps = { onAction(InventoryExportAction.IncludeAllApps) },
                    onAppClick = { onAction(InventoryExportAction.AppClicked(it)) },
                )

                InventoryExportStep.Columns -> InventoryColumnsStep(
                    selectedAttributes = state.selectedAttributes,
                    needsAppAnalysis = state.needsAppAnalysis,
                    needsUsageAccess = state.needsUsageAccess,
                    onAttributeToggle = { onAction(InventoryExportAction.AttributeToggled(it)) },
                    onGroupToggle = { onAction(InventoryExportAction.GroupToggled(it)) },
                    onAllToggle = { onAction(InventoryExportAction.AllAttributesToggled) },
                    onGrantUsageAccess = { onAction(InventoryExportAction.OpenUsageAccessSettings) },
                )

                InventoryExportStep.Export -> InventoryFormatStep(
                    appCount = state.appCount,
                    selectedAttributes = state.selectedAttributes,
                    format = state.format,
                    preview = state.preview,
                    isExporting = state.isExporting,
                    onFormatSelect = { onAction(InventoryExportAction.FormatSelected(it)) },
                    onEditColumns = { onAction(InventoryExportAction.StepSelected(InventoryExportStep.Columns)) },
                )
            }
        }

        InventoryExportBottomBar(
            state = state,
            onAction = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun InventoryStepIndicator(
    currentStep: InventoryExportStep,
    canReturn: Boolean,
    onStepSelect: (InventoryExportStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(bottom = 4.dp),
    ) {
        InventoryExportStep.entries.forEach { step ->
            val isReached = step <= currentStep
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .selectable(
                        selected = step == currentStep,
                        enabled = canReturn && step < currentStep,
                        role = Role.Tab,
                        onClick = { onStepSelect(step) },
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isReached) AppTheme.colors.primary else AppTheme.colors.surfaceVariant),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(step.labelRes()),
                    style = AppTheme.typography.labelMedium,
                    color = if (step == currentStep) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InventoryExportBottomBar(
    state: InventoryExportState,
    onAction: (InventoryExportAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = state.progress
    when {
        progress is InventoryExportProgressState.Running -> Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier,
        ) {
            Text(
                text = stringResource(R.string.inventory_export_progress, progress.processedApps, progress.totalApps),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurface,
            )
            ProgressBar(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                text = stringResource(R.string.inventory_export_cancel),
                onClick = { onAction(InventoryExportAction.CancelExport) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.step == InventoryExportStep.Export -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier,
        ) {
            OutlinedButton(
                text = stringResource(R.string.inventory_export_share),
                onClick = { onAction(InventoryExportAction.Share) },
                enabled = state.canContinue,
                modifier = Modifier.weight(1f),
            )
            Button(
                text = stringResource(R.string.inventory_export_save),
                onClick = { onAction(InventoryExportAction.Save) },
                enabled = state.canContinue,
                modifier = Modifier.weight(1f),
            )
        }

        else -> Button(
            text = stringResource(R.string.inventory_export_next),
            onClick = { onAction(InventoryExportAction.Continue) },
            enabled = state.canContinue,
            modifier = modifier,
        )
    }
}

private fun InventoryExportStep.labelRes(): Int = when (this) {
    InventoryExportStep.Apps -> R.string.inventory_export_step_apps
    InventoryExportStep.Columns -> R.string.inventory_export_step_columns
    InventoryExportStep.Export -> R.string.inventory_export_step_export
}

private fun InventoryExportFeedback.message(context: Context): String {
    val resources = context.resources
    return when (this) {
        is InventoryExportFeedback.Saved -> {
            val saved = resources.getQuantityString(R.plurals.inventory_export_saved, appCount, appCount, displayName)
            if (incompleteAppCount > 0) {
                saved + "\n" + resources.getQuantityString(R.plurals.inventory_export_incomplete, incompleteAppCount, incompleteAppCount)
            } else {
                saved
            }
        }

        is InventoryExportFeedback.SharedWithGaps ->
            resources.getQuantityString(R.plurals.inventory_export_incomplete, incompleteAppCount, incompleteAppCount)

        InventoryExportFeedback.Failed -> context.getString(R.string.inventory_export_failed)

        InventoryExportFeedback.DocumentPickerUnavailable -> context.getString(R.string.inventory_export_picker_unavailable)

        InventoryExportFeedback.ShareUnavailable -> context.getString(R.string.inventory_export_share_unavailable)
    }
}

@Preview
@Composable
private fun InventoryExportAppsStepPreview() {
    ApkAnalyzerTheme {
        InventoryExportContent(
            state = InventoryExportState(
                apps = AppListState.Content(previewAppListItems),
                isFilterActive = true,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun InventoryExportColumnsStepPreview() {
    ApkAnalyzerTheme {
        InventoryExportContent(
            state = InventoryExportState(
                step = InventoryExportStep.Columns,
                apps = AppListState.Content(previewAppListItems),
                isUsageAccessGranted = false,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun InventoryExportRunningPreview() {
    ApkAnalyzerTheme {
        InventoryExportContent(
            state = InventoryExportState(
                step = InventoryExportStep.Export,
                apps = AppListState.Content(previewAppListItems),
                preview = InventoryPreviewState.Content(PREVIEW_CSV, previewedAppCount = 2, isTruncated = false),
                progress = InventoryExportProgressState.Running(processedApps = 48, totalApps = 120),
            ),
            onAction = {},
        )
    }
}
