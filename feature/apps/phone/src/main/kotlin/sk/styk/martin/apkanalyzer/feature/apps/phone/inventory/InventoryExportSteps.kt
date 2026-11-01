package sk.styk.martin.apkanalyzer.feature.apps.phone.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttributeGroup
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Checkbox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Chip
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.apps.phone.R
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.AppListItemRowSkeleton
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.appitem.AppListItemRow
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.appitem.listItemShape
import sk.styk.martin.apkanalyzer.feature.apps.phone.list.AppListItem
import sk.styk.martin.apkanalyzer.feature.apps.phone.list.AppListState
import java.time.Instant

private val StepContentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)

@Composable
internal fun InventoryAppsStep(
    apps: AppListState,
    isFilterActive: Boolean,
    onEditFilters: () -> Unit,
    onIncludeAllApps: () -> Unit,
    onAppClick: (PackageName) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = StepContentPadding,
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "apps_header") {
            AppsStepHeader(
                apps = apps,
                isFilterActive = isFilterActive,
                onEditFilters = onEditFilters,
                onIncludeAllApps = onIncludeAllApps,
            )
        }

        when (apps) {
            AppListState.Loading -> itemsPositioned(
                count = 8,
                key = { "app_skeleton_$it" },
            ) { position ->
                AppListItemRowSkeleton(position = position)
            }

            is AppListState.Content -> if (apps.apps.isEmpty()) {
                item(key = "apps_empty") {
                    NoMatchingApps(
                        isFilterActive = isFilterActive,
                        onEditFilters = onEditFilters,
                        onIncludeAllApps = onIncludeAllApps,
                    )
                }
            } else {
                itemsPositioned(
                    items = apps.apps,
                    key = { _, app -> app.packageName.value },
                ) { position, app ->
                    AppListItemRow(
                        app = app,
                        onClick = { onAppClick(app.packageName) },
                        position = position,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppsStepHeader(
    apps: AppListState,
    isFilterActive: Boolean,
    onEditFilters: () -> Unit,
    onIncludeAllApps: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = when (apps) {
                AppListState.Loading -> stringResource(R.string.inventory_export_apps_loading)
                is AppListState.Content -> pluralStringResource(R.plurals.inventory_export_app_count, apps.apps.size, apps.apps.size)
            },
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                if (isFilterActive) R.string.inventory_export_apps_filtered else R.string.inventory_export_apps_all,
            ),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Chip(
                label = stringResource(R.string.inventory_export_edit_filters),
                onClick = onEditFilters,
                selected = isFilterActive,
                leadingIcon = ApkAnalyzerIcons.Filter,
            )
            if (isFilterActive) {
                TextButton(
                    text = stringResource(R.string.inventory_export_include_all_apps),
                    onClick = onIncludeAllApps,
                )
            }
        }
    }
}

@Composable
private fun NoMatchingApps(
    isFilterActive: Boolean,
    onEditFilters: () -> Unit,
    onIncludeAllApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.Search,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(if (isFilterActive) R.string.inventory_export_no_apps else R.string.inventory_export_no_apps_available),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onSurfaceVariant,
        )
        if (isFilterActive) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(
                    label = stringResource(R.string.inventory_export_change_filters),
                    onClick = onEditFilters,
                    leadingIcon = ApkAnalyzerIcons.Filter,
                )
                Chip(
                    label = stringResource(R.string.inventory_export_include_all_apps),
                    onClick = onIncludeAllApps,
                )
            }
        }
    }
}

@Composable
internal fun InventoryColumnsStep(
    selectedAttributes: Set<InventoryAttribute>,
    needsAppAnalysis: Boolean,
    needsUsageAccess: Boolean,
    onAttributeToggle: (InventoryAttribute) -> Unit,
    onGroupToggle: (InventoryAttributeGroup) -> Unit,
    onAllToggle: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                onToggle = onAllToggle,
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

        if (needsAppAnalysis) {
            item(key = "columns_hint_analysis") {
                InventoryHint(
                    icon = ApkAnalyzerIcons.HourglassEmpty,
                    text = stringResource(R.string.inventory_export_slow_hint),
                )
            }
        }

        if (needsUsageAccess) {
            item(key = "columns_hint_usage_access") {
                InventoryHint(
                    icon = ApkAnalyzerIcons.Warning,
                    text = stringResource(R.string.inventory_export_usage_access_hint),
                    action = {
                        TextButton(
                            text = stringResource(R.string.inventory_export_grant_access),
                            onClick = onGrantUsageAccess,
                        )
                    },
                )
            }
        }

        InventoryAttributeGroup.entries.forEach { group ->
            attributeGroupItems(
                group = group,
                selectedAttributes = selectedAttributes,
                onAttributeToggle = onAttributeToggle,
                onGroupToggle = onGroupToggle,
            )
        }
    }
}

private fun LazyListScope.attributeGroupItems(
    group: InventoryAttributeGroup,
    selectedAttributes: Set<InventoryAttribute>,
    onAttributeToggle: (InventoryAttribute) -> Unit,
    onGroupToggle: (InventoryAttributeGroup) -> Unit,
) {
    val attributes = group.attributes()
    item(key = "group_${group.name}") {
        SelectionHeader(
            title = stringResource(group.labelRes()),
            isAllSelected = selectedAttributes.containsAll(attributes),
            onToggle = { onGroupToggle(group) },
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
            onToggle = { onAttributeToggle(attribute) },
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
            .background(AppTheme.colors.surface, listItemShape(position))
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

@Composable
internal fun InventoryFormatStep(
    appCount: Int,
    selectedAttributes: Set<InventoryAttribute>,
    format: InventoryFormat,
    preview: InventoryPreviewState,
    isExporting: Boolean,
    onFormatSelect: (InventoryFormat) -> Unit,
    onEditColumns: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnCount = selectedAttributes.size
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(StepContentPadding),
    ) {
        Text(
            text = stringResource(
                R.string.inventory_export_summary,
                pluralStringResource(R.plurals.inventory_export_app_count, appCount, appCount),
                pluralStringResource(R.plurals.inventory_export_column_count, columnCount, columnCount),
            ),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onBackground,
        )

        SelectedColumns(
            selectedAttributes = selectedAttributes,
            isEditable = !isExporting,
            onEdit = onEditColumns,
        )

        SectionTitle(text = stringResource(R.string.inventory_export_format_title))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.selectableGroup(),
        ) {
            InventoryFormat.entries.forEach { option ->
                FormatOption(
                    format = option,
                    isSelected = option == format,
                    isEnabled = !isExporting,
                    onSelect = { onFormatSelect(option) },
                )
            }
        }

        SectionTitle(text = stringResource(R.string.inventory_export_preview_title))
        InventoryPreview(preview = preview)
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

internal val previewAppListItems = listOf(
    AppListItem(
        packageName = PackageName("com.instagram.android"),
        applicationName = "Instagram",
        targetSdk = 34,
        apkSize = 64.megabytes,
        totalSize = 510.megabytes,
        installTime = Instant.EPOCH,
        lastUpdateTime = Instant.EPOCH,
        lastUsedTime = Instant.EPOCH,
        source = AppSource.GooglePlay,
    ),
    AppListItem(
        packageName = PackageName("com.whatsapp"),
        applicationName = "WhatsApp",
        targetSdk = 33,
        apkSize = 32.megabytes,
        totalSize = null,
        installTime = Instant.EPOCH,
        lastUpdateTime = Instant.EPOCH,
        lastUsedTime = Instant.EPOCH,
        source = AppSource.GooglePlay,
    ),
)

internal const val PREVIEW_CSV = "app_name,package_name,version_name\nInstagram,com.instagram.android,312.0.0\nWhatsApp,com.whatsapp,2.24.1"

@Preview
@Composable
private fun InventoryFormatStepPreview() {
    ApkAnalyzerTheme {
        InventoryFormatStep(
            appCount = 120,
            selectedAttributes = InventoryAttribute.Defaults,
            format = InventoryFormat.Csv,
            preview = InventoryPreviewState.Content(PREVIEW_CSV, previewedAppCount = 2, isTruncated = true),
            isExporting = false,
            onFormatSelect = {},
            onEditColumns = {},
        )
    }
}

@Preview
@Composable
private fun InventoryAppsStepEmptyPreview() {
    ApkAnalyzerTheme {
        InventoryAppsStep(
            apps = AppListState.Content(emptyList()),
            isFilterActive = true,
            onEditFilters = {},
            onIncludeAllApps = {},
            onAppClick = {},
        )
    }
}
