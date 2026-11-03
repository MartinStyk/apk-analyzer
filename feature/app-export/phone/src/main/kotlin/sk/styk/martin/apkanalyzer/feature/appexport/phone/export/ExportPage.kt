package sk.styk.martin.apkanalyzer.feature.appexport.phone.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryPreset
import sk.styk.martin.apkanalyzer.core.appexport.format.InventoryFormat
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Chip
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.OutlinedButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ProgressBar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportAction
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportProgressState
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportState
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryPreviewState
import sk.styk.martin.apkanalyzer.feature.appexport.phone.R
import sk.styk.martin.apkanalyzer.feature.appexport.phone.apps.AppSelectionState
import sk.styk.martin.apkanalyzer.feature.appexport.phone.apps.AppSelectionSummary
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.CompactStepIndicator
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.PageContentPadding
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.StepGuidance
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.descriptionRes
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.labelRes

private const val VISIBLE_COLUMN_NAMES = 3

@Composable
internal fun ExportPage(
    state: InventoryExportState,
    onAction: (InventoryExportAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PageContentPadding),
    ) {
        CompactStepIndicator(
            currentStep = InventoryExportStep.Export,
            steps = state.steps,
            canSelectStep = !state.isExporting,
            onSelectStep = { onAction(InventoryExportAction.GoToStep(it)) },
            horizontalPadding = 0.dp,
        )
        StepGuidance(step = InventoryExportStep.Export)
        if (InventoryExportStep.Apps in state.steps) {
            SectionTitle(text = stringResource(R.string.inventory_export_apps_title))
            SummaryCard(
                value = appsValue(state.apps.summary),
                supporting = appsSupporting(state.apps.summary),
                enabled = !state.isExporting,
                onClick = { onAction(InventoryExportAction.GoToStep(InventoryExportStep.Apps)) },
            )
        }

        SectionTitle(text = stringResource(R.string.inventory_export_columns_title))
        SummaryCard(
            value = pluralStringResource(R.plurals.inventory_export_column_count, state.columns.size, state.columns.size),
            supporting = columnsSupporting(state.columns),
            enabled = !state.isExporting,
            onClick = { onAction(InventoryExportAction.GoToStep(InventoryExportStep.Columns)) },
        )

        SectionTitle(text = stringResource(R.string.inventory_export_format_title))
        FormatRow(
            state = state,
            onSelect = { onAction(InventoryExportAction.FormatSelected(it)) },
            onOpenReference = { onAction(InventoryExportAction.OpenFormatReference) },
        )

        SectionTitle(text = stringResource(R.string.inventory_export_preview_title))
        PreviewBox(preview = state.preview, hasColumns = state.exportedColumnCount > 0)
    }
}

@Composable
private fun appsValue(apps: AppSelectionSummary): String = if (apps.isLoaded) {
    pluralStringResource(R.plurals.inventory_export_app_count, apps.selectedCount, apps.selectedCount)
} else {
    stringResource(R.string.inventory_export_apps_loading)
}

@Composable
private fun appsSupporting(apps: AppSelectionSummary): String? {
    if (!apps.isLoaded) return null
    val leftOut = apps.leftOutCount
    val source = stringResource(if (apps.isFilterActive) R.string.inventory_export_apps_filtered else R.string.inventory_export_apps_all)
    val leftOutText = stringResource(R.string.inventory_export_apps_left_out, leftOut)
    return when {
        leftOut <= 0 -> source
        apps.isFilterActive -> "$source $leftOutText"
        else -> leftOutText
    }
}

@Composable
private fun columnsSupporting(columns: Set<InventoryColumn>): String? {
    val ordered = InventoryColumn.entries.filter { it in columns }
    if (ordered.isEmpty()) return stringResource(R.string.inventory_export_no_columns_hint)
    val names = ordered.take(VISIBLE_COLUMN_NAMES).map { stringResource(it.labelRes()) }.joinToString(separator = ", ")
    val remaining = ordered.size - VISIBLE_COLUMN_NAMES
    return if (remaining > 0) stringResource(R.string.inventory_export_columns_more, names, remaining) else names
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = 24.dp,
) {
    Text(
        text = text,
        style = AppTheme.typography.titleSmall,
        color = AppTheme.colors.onBackground,
        modifier = modifier.padding(top = topPadding, bottom = 8.dp),
    )
}

@Composable
private fun SummaryCard(
    value: String,
    supporting: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = ApkAnalyzerIcons.ChevronRight,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun FormatRow(
    state: InventoryExportState,
    onSelect: (InventoryFormat) -> Unit,
    onOpenReference: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InventoryFormat.entries.forEach { format ->
                Chip(
                    label = stringResource(format.labelRes()),
                    selected = format == state.format,
                    onClick = { if (!state.isExporting) onSelect(format) },
                )
            }
        }
        Text(
            text = stringResource(state.format.descriptionRes()),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        TextButton(
            text = stringResource(R.string.inventory_export_format_reference),
            onClick = onOpenReference,
            enabled = !state.isExporting,
        )
        if (state.omittedColumns.isNotEmpty()) {
            OmittedColumnsNotice(
                columns = state.omittedColumns,
                enabled = !state.isExporting,
                onUseJson = { onSelect(InventoryFormat.Json) },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun OmittedColumnsNotice(
    columns: List<InventoryColumn>,
    enabled: Boolean,
    onUseJson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surfaceVariant, Shapes.CardShape)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = ApkAnalyzerIcons.Info,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(
                    R.string.inventory_export_columns_omitted,
                    columns.map { stringResource(it.labelRes()) }.joinToString(separator = ", "),
                ),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                text = stringResource(R.string.inventory_export_use_json),
                onClick = onUseJson,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun PreviewBox(
    preview: InventoryPreviewState,
    hasColumns: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surfaceVariant, Shapes.CardShape)
            .padding(12.dp),
    ) {
        when {
            !hasColumns -> PreviewMessage(stringResource(R.string.inventory_export_no_columns_hint))

            preview is InventoryPreviewState.Loading -> LoadingSpinner(
                size = 24.dp,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
            )

            preview is InventoryPreviewState.Unavailable -> PreviewMessage(stringResource(R.string.inventory_export_preview_unavailable))

            preview is InventoryPreviewState.Content -> {
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
private fun PreviewMessage(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.bodyMedium,
        color = AppTheme.colors.onSurfaceVariant,
    )
}

@Composable
internal fun ExportBottomBar(
    state: InventoryExportState,
    onAction: (InventoryExportAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val progress = state.progress) {
        is InventoryExportProgressState.Running -> Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.inventory_export_progress,
                    progress.totalApps,
                    progress.processedApps,
                    progress.totalApps,
                ),
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

        InventoryExportProgressState.Idle -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                text = stringResource(R.string.inventory_export_share),
                onClick = { onAction(InventoryExportAction.Share) },
                enabled = state.canExport,
                modifier = Modifier.weight(1f),
            )
            Button(
                text = stringResource(R.string.inventory_export_save),
                onClick = { onAction(InventoryExportAction.Save) },
                enabled = state.canExport,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val previewApps = AppSelectionState(
    summary = AppSelectionSummary(
        isLoaded = true,
        candidateCount = 142,
        selectedCount = 140,
        isFilterActive = true,
    ),
)

private val previewContent = InventoryPreviewState.Content(
    text = "application_name,package_name,version_name\r\nChrome,com.android.chrome,126.0\r\nMaps,com.google.android.apps.maps,11.1",
    previewedAppCount = 3,
    isTruncated = true,
)

@Preview
@Composable
private fun ExportPagePreview() {
    ApkAnalyzerTheme {
        ExportPage(
            state = InventoryExportState(apps = previewApps, preview = previewContent),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ExportPageDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        ExportPage(
            state = InventoryExportState(apps = previewApps, preview = previewContent),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ExportPageExportingPreview() {
    ApkAnalyzerTheme {
        ExportPage(
            state = InventoryExportState(
                apps = previewApps,
                preview = previewContent,
                columns = InventoryPreset.Everything.columns,
                format = InventoryFormat.Json,
                progress = InventoryExportProgressState.Running(processedApps = 57, totalApps = 140),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ExportPageLoadingPreview() {
    ApkAnalyzerTheme {
        ExportPage(
            state = InventoryExportState(),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ExportPageOmittedColumnsPreview() {
    ApkAnalyzerTheme {
        ExportPage(
            state = InventoryExportState(
                step = InventoryExportStep.Export,
                apps = previewApps,
                preview = previewContent,
                columns = InventoryPreset.Basics.columns + InventoryColumn.Activities + InventoryColumn.SigningCertificates,
            ),
            onAction = {},
        )
    }
}
