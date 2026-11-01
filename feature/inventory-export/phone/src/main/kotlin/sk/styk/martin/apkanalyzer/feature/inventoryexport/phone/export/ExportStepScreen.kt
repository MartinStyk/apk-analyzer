package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.OutlinedButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ProgressBar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.launcher.launchSafely
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.R
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportScaffold
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.StepContentPadding
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.descriptionRes
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.labelRes

@Composable
internal fun ExportStepScreen(
    onBack: () -> Unit,
    onSelectStep: (InventoryExportStep) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportStepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val csvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(InventoryFormat.Csv.mimeType),
    ) { destination ->
        if (destination != null) viewModel.onAction(ExportStepAction.SaveTo(destination, InventoryFormat.Csv))
    }
    val jsonDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(InventoryFormat.Json.mimeType),
    ) { destination ->
        if (destination != null) viewModel.onAction(ExportStepAction.SaveTo(destination, InventoryFormat.Json))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ExportStepEvent.NavigateBack -> onBack()

                is ExportStepEvent.NavigateToStep -> onSelectStep(event.step)

                is ExportStepEvent.CreateDocument -> {
                    val launcher = when (event.format) {
                        InventoryFormat.Csv -> csvDocumentLauncher
                        InventoryFormat.Json -> jsonDocumentLauncher
                    }
                    launcher.launchSafely(event.fileName) {
                        viewModel.onAction(ExportStepAction.DocumentPickerUnavailable)
                    }
                }

                is ExportStepEvent.ShareFile -> try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        clipData = ClipData.newRawUri(null, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                } catch (_: ActivityNotFoundException) {
                    viewModel.onAction(ExportStepAction.ShareUnavailable)
                }

                is ExportStepEvent.ShowFeedback ->
                    Toast.makeText(context, event.feedback.message(context), Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler {
        viewModel.onAction(ExportStepAction.NavigateBack)
    }

    ExportStepContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun ExportStepContent(
    state: ExportStepState,
    onAction: (ExportStepAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    InventoryExportScaffold(
        currentStep = InventoryExportStep.Export,
        onBack = { onAction(ExportStepAction.NavigateBack) },
        onSelectStep = { onAction(ExportStepAction.StepSelected(it)) },
        canSelectStep = !state.isExporting,
        bottomBar = {
            ExportBottomBar(
                state = state,
                onAction = onAction,
            )
        },
        modifier = modifier,
    ) {
        ExportOptions(
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun ExportOptions(
    state: ExportStepState,
    onAction: (ExportStepAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnCount = state.selectedAttributes.size
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(StepContentPadding),
    ) {
        Text(
            text = stringResource(
                R.string.inventory_export_summary,
                pluralStringResource(R.plurals.inventory_export_app_count, state.appCount, state.appCount),
                pluralStringResource(R.plurals.inventory_export_column_count, columnCount, columnCount),
            ),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onBackground,
        )

        SelectedColumns(
            selectedAttributes = state.selectedAttributes,
            isEditable = !state.isExporting,
            onEdit = { onAction(ExportStepAction.StepSelected(InventoryExportStep.Columns)) },
        )

        SectionTitle(text = stringResource(R.string.inventory_export_format_title))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.selectableGroup(),
        ) {
            InventoryFormat.entries.forEach { option ->
                FormatOption(
                    format = option,
                    isSelected = option == state.format,
                    isEnabled = !state.isExporting,
                    onSelect = { onAction(ExportStepAction.FormatSelected(option)) },
                )
            }
        }

        SectionTitle(text = stringResource(R.string.inventory_export_preview_title))
        InventoryPreview(preview = state.preview)
    }
}

@Composable
private fun SelectedColumns(
    selectedAttributes: Set<InventoryAttribute>,
    isEditable: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.inventory_export_step_columns),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(R.string.inventory_export_change),
                onClick = onEdit,
                enabled = isEditable,
            )
        }
        Text(
            text = InventoryAttribute.entries
                .filter { it in selectedAttributes }
                .map { stringResource(it.labelRes()) }
                .joinToString(separator = ", "),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.titleSmall,
        color = AppTheme.colors.onBackground,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun FormatOption(
    format: InventoryFormat,
    isSelected: Boolean,
    isEnabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(if (isSelected) AppTheme.colors.secondaryContainer else AppTheme.colors.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.pillBorder,
                shape = Shapes.CardShape,
            )
            .selectable(selected = isSelected, enabled = isEnabled, role = Role.RadioButton, onClick = onSelect)
            .padding(16.dp),
    ) {
        Icon(
            imageVector = format.icon(),
            tint = if (isSelected) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(format.labelRes()),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
            )
            Text(
                text = stringResource(format.descriptionRes()),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}

private fun InventoryFormat.icon(): ImageVector = when (this) {
    InventoryFormat.Csv -> ApkAnalyzerIcons.Table
    InventoryFormat.Json -> ApkAnalyzerIcons.DataObject
}

@Composable
private fun InventoryPreview(preview: InventoryPreviewState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surfaceVariant, Shapes.CardShape)
            .padding(12.dp),
    ) {
        when (preview) {
            InventoryPreviewState.Loading -> LoadingSpinner(
                size = 24.dp,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
            )

            InventoryPreviewState.Unavailable -> Text(
                text = stringResource(R.string.inventory_export_preview_unavailable),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )

            is InventoryPreviewState.Content -> {
                Text(
                    text = preview.text,
                    style = AppTheme.typography.monospace,
                    color = AppTheme.colors.onSurface,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
                if (preview.isTruncated) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.inventory_export_preview_caption,
                            preview.previewedAppCount,
                            preview.previewedAppCount,
                        ),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportBottomBar(
    state: ExportStepState,
    onAction: (ExportStepAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = state.progress
    when (progress) {
        is InventoryExportProgressState.Running -> Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxWidth(),
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
                onClick = { onAction(ExportStepAction.CancelExport) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        InventoryExportProgressState.Idle -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                text = stringResource(R.string.inventory_export_share),
                onClick = { onAction(ExportStepAction.Share) },
                enabled = state.canExport,
                modifier = Modifier.weight(1f),
            )
            Button(
                text = stringResource(R.string.inventory_export_save),
                onClick = { onAction(ExportStepAction.Save) },
                enabled = state.canExport,
                modifier = Modifier.weight(1f),
            )
        }
    }
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

private const val PREVIEW_CSV = "app_name,package_name,version_name\nInstagram,com.instagram.android,312.0.0\nWhatsApp,com.whatsapp,2.24.1"

@Preview
@Composable
private fun ExportStepContentPreview() {
    ApkAnalyzerTheme {
        ExportStepContent(
            state = ExportStepState(
                appCount = 120,
                preview = InventoryPreviewState.Content(PREVIEW_CSV, previewedAppCount = 2, isTruncated = true),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ExportStepRunningPreview() {
    ApkAnalyzerTheme {
        ExportStepContent(
            state = ExportStepState(
                appCount = 120,
                format = InventoryFormat.Json,
                preview = InventoryPreviewState.Loading,
                progress = InventoryExportProgressState.Running(processedApps = 48, totalApps = 120),
            ),
            onAction = {},
        )
    }
}
