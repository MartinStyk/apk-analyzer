package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.columns

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttributeGroup
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Checkbox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.shape
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.R
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportScaffold
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.StepContentPadding
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.labelRes

@Composable
internal fun ColumnsStepScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSelectStep: (InventoryExportStep) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ColumnsStepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ColumnsStepEvent.NavigateBack -> onBack()
                ColumnsStepEvent.NavigateToExport -> onNext()
                is ColumnsStepEvent.NavigateToStep -> onSelectStep(event.step)
                ColumnsStepEvent.OpenUsageAccessSettings -> context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
    }

    ColumnsStepContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun ColumnsStepContent(
    state: ColumnsStepState,
    onAction: (ColumnsStepAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    InventoryExportScaffold(
        currentStep = InventoryExportStep.Columns,
        onBack = { onAction(ColumnsStepAction.NavigateBack) },
        onSelectStep = { onAction(ColumnsStepAction.StepSelected(it)) },
        bottomBar = {
            Button(
                text = stringResource(R.string.inventory_export_next),
                onClick = { onAction(ColumnsStepAction.Continue) },
                enabled = state.canContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = modifier,
    ) {
        ColumnsList(
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun ColumnsList(
    state: ColumnsStepState,
    onAction: (ColumnsStepAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedAttributes = state.selectedAttributes
    LazyColumn(
        contentPadding = StepContentPadding,
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "columns_header") {
            SelectionHeader(
                title = stringResource(
                    R.string.inventory_export_columns_selected,
                    selectedAttributes.size,
                    InventoryAttribute.entries.size,
                ),
                isAllSelected = selectedAttributes.size == InventoryAttribute.entries.size,
                onToggle = { onAction(ColumnsStepAction.AllAttributesToggled) },
                titleStyle = TitleStyle.Screen,
            )
        }

        if (selectedAttributes.isEmpty()) {
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

        if (state.needsUsageAccess) {
            item(key = "columns_hint_usage_access") {
                InventoryHint(
                    icon = ApkAnalyzerIcons.Warning,
                    text = stringResource(R.string.inventory_export_usage_access_hint),
                    action = {
                        TextButton(
                            text = stringResource(R.string.inventory_export_grant_access),
                            onClick = { onAction(ColumnsStepAction.OpenUsageAccessSettings) },
                        )
                    },
                )
            }
        }

        InventoryAttributeGroup.entries.forEach { group ->
            attributeGroupItems(
                group = group,
                selectedAttributes = selectedAttributes,
                onAction = onAction,
            )
        }
    }
}

private fun LazyListScope.attributeGroupItems(
    group: InventoryAttributeGroup,
    selectedAttributes: Set<InventoryAttribute>,
    onAction: (ColumnsStepAction) -> Unit,
) {
    val attributes = InventoryAttribute.entries.filter { it.group == group }
    item(key = "group_${group.name}") {
        SelectionHeader(
            title = stringResource(group.labelRes()),
            isAllSelected = selectedAttributes.containsAll(attributes),
            onToggle = { onAction(ColumnsStepAction.GroupToggled(group)) },
            titleStyle = TitleStyle.Section,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
    itemsPositioned(
        items = attributes,
        key = { _, attribute -> attribute.key },
    ) { position, attribute ->
        AttributeRow(
            attribute = attribute,
            isSelected = attribute in selectedAttributes,
            position = position,
            onToggle = { onAction(ColumnsStepAction.AttributeToggled(attribute)) },
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
private fun AttributeRow(
    attribute: InventoryAttribute,
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
        Text(
            text = stringResource(attribute.labelRes()),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (attribute.requiresAppAnalysis) {
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
private fun ColumnsStepContentPreview() {
    ApkAnalyzerTheme {
        ColumnsStepContent(
            state = ColumnsStepState(
                selectedAttributes = InventoryAttribute.Defaults + InventoryAttribute.LastUsed + InventoryAttribute.ActivityCount,
                isUsageAccessGranted = false,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ColumnsStepNothingSelectedPreview() {
    ApkAnalyzerTheme {
        ColumnsStepContent(
            state = ColumnsStepState(selectedAttributes = emptySet()),
            onAction = {},
        )
    }
}
