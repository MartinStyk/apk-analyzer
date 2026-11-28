package sk.styk.martin.apkanalyzer.feature.appexport.phone.columns

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import sk.styk.martin.apkanalyzer.core.appexport.api.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.api.column.InventoryColumnGroup
import sk.styk.martin.apkanalyzer.core.appexport.api.column.InventoryColumnShape
import sk.styk.martin.apkanalyzer.core.appexport.api.column.InventoryPreset
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Checkbox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Chip
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.IconButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.InfoCard
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ProBadge
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.SwipeDismiss
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.shape
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportAction
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportHint
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportState
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.appexport.phone.ProOwnership
import sk.styk.martin.apkanalyzer.feature.appexport.phone.R
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.CompactStepIndicator
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
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
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
            StepGuidance(step = InventoryExportStep.Columns, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
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
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
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
                selectAllLabel = stringResource(R.string.inventory_export_select_all_details),
                clearLabel = stringResource(R.string.inventory_export_clear_all_details),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (selectedColumns.isEmpty()) {
            item(key = "columns_hint_empty") {
                InventoryHint(
                    icon = ApkAnalyzerIcons.Info,
                    text = stringResource(R.string.inventory_export_no_columns_hint),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (state.hasStructuredColumns && InventoryExportHint.Structured !in state.dismissedHints) {
            item(key = "columns_hint_structured") {
                DismissibleInventoryHint(
                    icon = ApkAnalyzerIcons.Info,
                    text = stringResource(R.string.inventory_export_structured_hint),
                    onDismiss = { onAction(InventoryExportAction.DismissHint(InventoryExportHint.Structured)) },
                )
            }
        }

        if (state.needsUsageAccess && InventoryExportHint.UsageAccess !in state.dismissedHints) {
            item(key = "columns_hint_usage_access") {
                DismissibleInventoryHint(
                    icon = ApkAnalyzerIcons.Warning,
                    text = stringResource(R.string.inventory_export_usage_access_hint),
                    onDismiss = { onAction(InventoryExportAction.DismissHint(InventoryExportHint.UsageAccess)) },
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
                showProBadges = state.proOwnership == ProOwnership.NotOwned,
                onAction = onAction,
            )
        }
    }
}

private fun LazyListScope.columnGroupItems(
    group: InventoryColumnGroup,
    selectedColumns: Set<InventoryColumn>,
    showProBadges: Boolean,
    onAction: (InventoryExportAction) -> Unit,
) {
    val columns = InventoryColumn.entries.filter { it.group == group }
    val isFirstGroup = group == InventoryColumnGroup.entries.first()
    item(key = "group_${group.name}") {
        SelectionHeader(
            title = stringResource(group.labelRes()),
            isAllSelected = selectedColumns.containsAll(columns),
            onToggle = { onAction(InventoryExportAction.ColumnGroupToggled(group)) },
            titleStyle = TitleStyle.Section,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = if (isFirstGroup) 0.dp else 16.dp),
        )
    }
    itemsPositioned(
        items = columns,
        key = { _, column -> column.key },
    ) { position, column ->
        ColumnRow(
            column = column,
            isSelected = column in selectedColumns,
            isProLocked = showProBadges && column !in InventoryPreset.Basics.columns,
            position = position,
            onToggle = { onAction(InventoryExportAction.ColumnToggled(column)) },
            onOpenPro = { onAction(InventoryExportAction.OpenPro) },
            modifier = Modifier.padding(horizontal = 16.dp),
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
    selectAllLabel: String = stringResource(R.string.inventory_export_select_all),
    clearLabel: String = stringResource(R.string.inventory_export_clear),
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
            text = if (isAllSelected) clearLabel else selectAllLabel,
            onClick = onToggle,
        )
    }
}

@Composable
private fun ColumnRow(
    column: InventoryColumn,
    isSelected: Boolean,
    isProLocked: Boolean,
    position: ListItemPosition,
    onToggle: () -> Unit,
    onOpenPro: () -> Unit,
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
        if (isProLocked) {
            Spacer(modifier = Modifier.width(8.dp))
            ProBadge(onClick = onOpenPro)
        }
    }
}

@Composable
private fun InventoryHint(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    InfoCard(
        icon = icon,
        title = null,
        body = text,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
private fun DismissibleInventoryHint(
    icon: ImageVector,
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    SwipeDismiss(
        onDismiss = onDismiss,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
    ) {
        InfoCard(
            icon = icon,
            title = null,
            body = text,
            action = action,
            trailingAction = {
                IconButton(
                    imageVector = ApkAnalyzerIcons.Clear,
                    contentDescription = stringResource(R.string.inventory_export_dismiss_hint),
                    onClick = onDismiss,
                )
            },
        )
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
                proOwnership = ProOwnership.NotOwned,
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
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
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
