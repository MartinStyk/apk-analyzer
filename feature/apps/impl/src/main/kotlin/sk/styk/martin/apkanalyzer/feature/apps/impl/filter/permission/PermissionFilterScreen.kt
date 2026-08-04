package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.permission

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import sk.styk.martin.apkanalyzer.core.apppermissions.model.DevicePermission
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Checkbox
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.OutlinedChip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.TextButton
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.uilibrary.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.apps.impl.R
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.PermissionPreset

@Composable
internal fun PermissionFilterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionFilterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PermissionFilterEvent.NavigateBack -> onBack()
            }
        }
    }

    BackHandler {
        viewModel.onAction(PermissionFilterAction.NavigateBack)
    }

    PermissionFilterContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun PermissionFilterContent(
    state: PermissionFilterState,
    onAction: (PermissionFilterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPresetsSheet by remember { mutableStateOf(false) }
    var showMatchSheet by remember { mutableStateOf(false) }
    var showShowSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        ) {
            IconButton(
                imageVector = ApkAnalyzerIcons.Back,
                onClick = { onAction(PermissionFilterAction.NavigateBack) },
                contentDescription = stringResource(R.string.content_description_back),
            )
            SearchBarActive(
                query = state.searchQuery,
                placeholder = stringResource(R.string.filter_permissions_search_hint),
                onQueryChange = { onAction(PermissionFilterAction.SearchQueryChanged(it)) },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(R.string.filter_reset),
                onClick = { onAction(PermissionFilterAction.Reset) },
            )
        }

        val activePresetCount = state.presets.count { it.isSelected }
        val presetsLabel = when (activePresetCount) {
            0 -> stringResource(R.string.filter_permissions_chip_presets)
            1 -> state.presets.first { it.isSelected }.preset.displayLabel()
            else -> stringResource(R.string.filter_permissions_chip_presets_count, activePresetCount)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
        ) {
            OutlinedChip(
                label = presetsLabel,
                selected = activePresetCount > 0,
                trailingIcon = ApkAnalyzerIcons.ArrowDropDown,
                onClick = { showPresetsSheet = true },
            )
            OutlinedChip(
                label = when (state.matchMode) {
                    MatchMode.Any -> stringResource(R.string.filter_permissions_chip_match_any)
                    MatchMode.All -> stringResource(R.string.filter_permissions_chip_match_all)
                },
                selected = state.matchMode == MatchMode.All,
                trailingIcon = ApkAnalyzerIcons.ArrowDropDown,
                onClick = { showMatchSheet = true },
            )
            OutlinedChip(
                label = if (state.showOnlySelected) {
                    stringResource(R.string.filter_permissions_chip_show_selected)
                } else {
                    stringResource(R.string.filter_permissions_chip_show_all)
                },
                selected = state.showOnlySelected,
                trailingIcon = ApkAnalyzerIcons.ArrowDropDown,
                onClick = { showShowSheet = true },
            )
        }

        when (val listState = state.permissionListState) {
            PermissionListState.Loading -> {
                LoadingSpinner(modifier = Modifier.fillMaxSize())
            }

            is PermissionListState.Permissions -> {
                if (listState.items.isEmpty()) {
                    PermissionListEmptyState(
                        searchQuery = state.searchQuery,
                        showOnlySelected = state.showOnlySelected,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsPositioned(
                            items = listState.items,
                            key = { _, item -> item.permission.name },
                        ) { position, item ->
                            PermissionRow(
                                item = item,
                                position = position,
                                onToggle = { onAction(PermissionFilterAction.PermissionToggled(item.permission.name)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPresetsSheet) {
        PresetsBottomSheet(
            presets = state.presets,
            onPresetToggle = { onAction(PermissionFilterAction.PresetToggled(it)) },
            onDismiss = { showPresetsSheet = false },
        )
    }
    if (showMatchSheet) {
        MatchBottomSheet(
            matchMode = state.matchMode,
            onMatchModeChange = {
                onAction(PermissionFilterAction.MatchModeChanged(it))
                showMatchSheet = false
            },
            onDismiss = { showMatchSheet = false },
        )
    }
    if (showShowSheet) {
        ShowBottomSheet(
            showOnlySelected = state.showOnlySelected,
            onShowOnlySelectedChange = {
                onAction(PermissionFilterAction.ShowOnlySelectedToggled)
                showShowSheet = false
            },
            onShowAllChange = {
                if (state.showOnlySelected) onAction(PermissionFilterAction.ShowOnlySelectedToggled)
                showShowSheet = false
            },
            onDismiss = { showShowSheet = false },
        )
    }
}

@Composable
private fun PermissionRow(
    item: PermissionItem,
    position: ListItemPosition,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape: Shape = when (position) {
        ListItemPosition.Single -> Shapes.CardShape
        ListItemPosition.First -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ListItemPosition.Middle -> RectangleShape
        ListItemPosition.Last -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, shape)
            .padding(2.dp)
            .clip(Shapes.CardShape)
            .clickable(onClick = onToggle)
            .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Checkbox(checked = item.isSelected, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.permission.label,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurface,
            )
            Text(
                text = item.permission.name,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PresetsBottomSheet(
    presets: ImmutableList<PermissionPresetState>,
    onPresetToggle: (PermissionPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.filter_permissions_chip_presets),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = stringResource(R.string.filter_permissions_presets_description),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            presets.forEach { uiPreset ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPresetToggle(uiPreset.preset) }
                        .padding(vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = uiPreset.isSelected,
                        onCheckedChange = { onPresetToggle(uiPreset.preset) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiPreset.preset.displayLabel(),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchBottomSheet(
    matchMode: MatchMode,
    onMatchModeChange: (MatchMode) -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.filter_permissions_match_label),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = stringResource(R.string.filter_permissions_match_description),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            BottomSheetOptionRow(
                label = stringResource(R.string.filter_permissions_match_any),
                sublabel = stringResource(R.string.filter_permissions_match_any_subtitle),
                checked = matchMode == MatchMode.Any,
                onClick = { onMatchModeChange(MatchMode.Any) },
            )
            BottomSheetOptionRow(
                label = stringResource(R.string.filter_permissions_match_all),
                sublabel = stringResource(R.string.filter_permissions_match_all_subtitle),
                checked = matchMode == MatchMode.All,
                onClick = { onMatchModeChange(MatchMode.All) },
            )
        }
    }
}

@Composable
private fun ShowBottomSheet(
    showOnlySelected: Boolean,
    onShowAllChange: () -> Unit,
    onShowOnlySelectedChange: () -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.filter_permissions_sheet_show),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = stringResource(R.string.filter_permissions_show_description),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            BottomSheetOptionRow(
                label = stringResource(R.string.filter_permissions_show_all),
                sublabel = stringResource(R.string.filter_permissions_show_all_subtitle),
                checked = !showOnlySelected,
                onClick = onShowAllChange,
            )
            BottomSheetOptionRow(
                label = stringResource(R.string.filter_permissions_show_selected),
                sublabel = stringResource(R.string.filter_permissions_show_selected_subtitle),
                checked = showOnlySelected,
                onClick = onShowOnlySelectedChange,
            )
        }
    }
}

@Composable
private fun BottomSheetOptionRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
            if (sublabel != null) {
                Text(text = sublabel, style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PermissionPreset.displayLabel(): String = when (this) {
    PermissionPreset.Sensitive -> stringResource(R.string.filter_permissions_preset_sensitive)
    PermissionPreset.Camera -> stringResource(R.string.filter_permissions_preset_camera)
    PermissionPreset.Microphone -> stringResource(R.string.filter_permissions_preset_microphone)
    PermissionPreset.Location -> stringResource(R.string.filter_permissions_preset_location)
    PermissionPreset.Contacts -> stringResource(R.string.filter_permissions_preset_contacts)
    PermissionPreset.Sms -> stringResource(R.string.filter_permissions_preset_sms)
    PermissionPreset.Storage -> stringResource(R.string.filter_permissions_preset_storage)
}

@Composable
private fun PermissionListEmptyState(
    searchQuery: String,
    showOnlySelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            val (icon, title, subtitle) = when {
                searchQuery.isNotBlank() -> Triple(
                    ApkAnalyzerIcons.Search,
                    stringResource(R.string.filter_permissions_empty_search_title, searchQuery),
                    stringResource(R.string.filter_permissions_empty_search_subtitle),
                )

                showOnlySelected -> Triple(
                    ApkAnalyzerIcons.Lock,
                    stringResource(R.string.filter_permissions_empty_selected_title),
                    stringResource(R.string.filter_permissions_empty_selected_subtitle),
                )

                else -> Triple(
                    ApkAnalyzerIcons.Permissions,
                    stringResource(R.string.filter_permissions_empty_title),
                    stringResource(R.string.filter_permissions_empty_subtitle),
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = title,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun PermissionFilterLoadingPreview() {
    ApkAnalyzerTheme {
        PermissionFilterContent(
            state = PermissionFilterState(),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PermissionFilterWithSelectionPreview() {
    ApkAnalyzerTheme {
        PermissionFilterContent(
            state = PermissionFilterState(
                matchMode = MatchMode.All,
                presets = PermissionPreset.all
                    .map { PermissionPresetState(it, false) }
                    .toImmutableList(),
                permissionListState = PermissionListState.Permissions(
                    items = persistentListOf(
                        PermissionItem(DevicePermission("android.permission.CAMERA", "Camera"), isSelected = true),
                        PermissionItem(DevicePermission("android.permission.RECORD_AUDIO", "Microphone"), isSelected = true),
                        PermissionItem(DevicePermission("android.permission.ACCESS_FINE_LOCATION", "Precise Location"), isSelected = false),
                        PermissionItem(DevicePermission("android.permission.INTERNET", "Internet"), isSelected = false),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PermissionFilterEmptyPreview() {
    ApkAnalyzerTheme {
        PermissionFilterContent(
            state = PermissionFilterState(
                searchQuery = "camera",
                permissionListState = PermissionListState.Permissions(items = persistentListOf()),
                presets = PermissionPreset.all.map { PermissionPresetState(it, false) }.toImmutableList(),
            ),
            onAction = {},
        )
    }
}
