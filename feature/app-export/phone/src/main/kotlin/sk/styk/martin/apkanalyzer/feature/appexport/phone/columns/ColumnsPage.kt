package sk.styk.martin.apkanalyzer.feature.appexport.phone.columns

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumnGroup
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumnShape
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryPreset
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Checkbox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Chip
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.shape
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportAction
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportState
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.appexport.phone.R
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.CompactStepIndicator
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.PageContentPadding
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.StepGuidance
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.descriptionRes
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.labelRes

@Composable
internal fun ColumnsPage(
    state: InventoryExportState,
    onAction: (InventoryExportAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedColumns = state.columns
    LazyColumn(
        contentPadding = PageContentPadding,
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "step_indicator") {
            CompactStepIndicator(
                currentStep = InventoryExportStep.Columns,
                steps = state.steps,
                canSelectStep = true,
                onSelectStep = { onAction(InventoryExportAction.GoToStep(it)) },
            )
        }

        item(key = "columns_guidance") {
            StepGuidance(step = InventoryExportStep.Columns, modifier = Modifier.padding(bottom = 12.dp))
        }

        item(key = "columns_presets") {
            Column {
                PresetRow(
                    selectedPreset = state.preset,
                    onSelect = { onAction(InventoryExportAction.PresetSelected(it)) },
                )
                state.preset?.let { preset ->
                    Text(
                        text = stringResource(preset.descriptionRes()),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        item(key = "columns_header") {
            SelectionHeader(
                title = stringResource(R.string.inventory_export_columns_selected, selectedColumns.size, InventoryColumn.entries.size),
                isAllSelected = selectedColumns.size == InventoryColumn.entries.size,
                onToggle = { onAction(InventoryExportAction.AllColumnsToggled) },
                titleStyle = TitleStyle.Screen,
            )
        }

        if (selectedColumns.isEmpty()) {
            item(key = "columns_hint_empty") {
                InventoryHint(
                    icon = ApkAnalyzerIcons.Info,
                    text = stringResource(R.string.inventory_export_no_columns_hint),
                )
            }
        }

        if (state.needsAppAnalysis) {
            item(key = "columns_hint_analysis") {
                InventoryHint(
                    icon = ApkAnalyzerIcons.HourglassEmpty,
                    text = stringResource(R.string.inventory_export_slow_hint),
                )
            }
        }

        if (state.hasStructuredColumns) {
            item(key = "columns_hint_structured") {
                InventoryHint(
                    icon = ApkAnalyzerIcons.Info,
                    text = stringResource(R.string.inventory_export_structured_hint),
                )
            }
        }

        if (state.needsUsageAccess) {
            item(key = "columns_hint_usage_access") {
                InventoryHint(
                    icon = ApkAnalyzerIcons.Warning,
                    text = stringResource(R.string.inventory_export_usage_access_hint),
                    action = {
                        TextButton(
                            text = stringResource(R.string.inventory_export_grant_access),
                            onClick = { onAction(InventoryExportAction.OpenUsageAccessSettings) },
                        )
                    },
                )
            }
        }

        InventoryColumnGroup.entries.forEach { group ->
            columnGroupItems(
                group = group,
                selectedColumns = selectedColumns,
                onAction = onAction,
            )
        }
    }
}

private fun LazyListScope.columnGroupItems(
    group: InventoryColumnGroup,
    selectedColumns: Set<InventoryColumn>,
    onAction: (InventoryExportAction) -> Unit,
) {
    val columns = InventoryColumn.entries.filter { it.group == group }
    item(key = "group_${group.name}") {
        SelectionHeader(
            title = stringResource(group.labelRes()),
            isAllSelected = selectedColumns.containsAll(columns),
            onToggle = { onAction(InventoryExportAction.ColumnGroupToggled(group)) },
            titleStyle = TitleStyle.Section,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
    itemsPositioned(
        items = columns,
        key = { _, column -> column.key },
    ) { position, column ->
        ColumnRow(
            column = column,
            isSelected = column in selectedColumns,
            position = position,
            onToggle = { onAction(InventoryExportAction.ColumnToggled(column)) },
        )
    }
}

private enum class TitleStyle { Screen, Section }

@Composable
private fun SelectionHeader(
    title: String,
    isAllSelected: Boolean,
    onToggle: () -> Unit,
    titleStyle: TitleStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = when (titleStyle) {
                TitleStyle.Screen -> AppTheme.typography.titleMedium
                TitleStyle.Section -> AppTheme.typography.titleSmall
            },
            color = AppTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            text = stringResource(if (isAllSelected) R.string.inventory_export_clear else R.string.inventory_export_select_all),
            onClick = onToggle,
        )
    }
}

@Composable
private fun ColumnRow(
    column: InventoryColumn,
    isSelected: Boolean,
    position: ListItemPosition,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, position.shape())
            .padding(2.dp)
            .clip(Shapes.CardShape)
            .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(column.labelRes()),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurface,
            )
            if (column.shape == InventoryColumnShape.Structured) {
                Tag(text = stringResource(R.string.inventory_export_json_only))
            }
        }
        if (column.requiresAppAnalysis) {
            Tag(text = stringResource(R.string.inventory_export_slower_tag))
        }
    }
}

@Composable
private fun InventoryHint(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(AppTheme.colors.surfaceVariant, Shapes.CardShape)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = if (action == null) 12.dp else 4.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        if (action != null) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                action()
            }
        }
    }
}

@Preview
@Composable
private fun ColumnsPagePreview() {
    ApkAnalyzerTheme {
        ColumnsPage(
            state = InventoryExportState(
                columns = InventoryPreset.SecurityAudit.columns + InventoryColumn.LastUsed + InventoryColumn.Activities,
                isUsageAccessGranted = false,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ColumnsPageNothingSelectedPreview() {
    ApkAnalyzerTheme {
        ColumnsPage(
            state = InventoryExportState(columns = emptySet()),
            onAction = {},
        )
    }
}

@Composable
private fun PresetRow(
    selectedPreset: InventoryPreset?,
    onSelect: (InventoryPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        InventoryPreset.entries.forEach { preset ->
            Chip(
                label = stringResource(preset.labelRes()),
                selected = preset == selectedPreset,
                onClick = { onSelect(preset) },
            )
        }
    }
}
