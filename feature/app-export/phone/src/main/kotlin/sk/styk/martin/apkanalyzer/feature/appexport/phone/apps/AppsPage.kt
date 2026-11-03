package sk.styk.martin.apkanalyzer.feature.appexport.phone.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.AppRow
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Checkbox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.IconButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.IconButtonStyle
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportAction
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportState
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.appexport.phone.R
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.CompactStepIndicator
import sk.styk.martin.apkanalyzer.feature.appexport.phone.components.StepGuidance

@Composable
internal fun AppsPage(
    state: InventoryExportState,
    onAction: (InventoryExportAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apps = state.apps
    val showIntro = state.showIntro
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "step_indicator") {
            CompactStepIndicator(
                currentStep = InventoryExportStep.Apps,
                steps = state.steps,
                canSelectStep = true,
                onSelectStep = { onAction(InventoryExportAction.GoToStep(it)) },
            )
        }
        item(key = "intro_or_guidance") {
            if (showIntro) {
                IntroCard(
                    onDismiss = { onAction(InventoryExportAction.DismissIntro) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
            } else {
                StepGuidance(
                    step = InventoryExportStep.Apps,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        item(key = "search") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SearchBarActive(
                    query = apps.query,
                    placeholder = stringResource(R.string.inventory_export_search_apps),
                    sharedElementKey = null,
                    onQueryChange = { onAction(InventoryExportAction.SearchQueryChanged(it)) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    imageVector = ApkAnalyzerIcons.Filter,
                    style = if (apps.summary.isFilterActive) IconButtonStyle.Highlighted else IconButtonStyle.Filled,
                    onClick = { onAction(InventoryExportAction.EditFilters) },
                    contentDescription = stringResource(R.string.inventory_export_edit_filters),
                )
            }
        }
        when (val items = apps.items) {
            InventoryAppList.Loading -> item(key = "loading") {
                LoadingSpinner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                )
            }

            is InventoryAppList.Content -> {
                item(key = "selection_header") {
                    SelectionHeader(
                        apps = apps,
                        hasVisibleApps = items.apps.isNotEmpty(),
                        onToggleVisible = { onAction(InventoryExportAction.VisibleAppsToggled) },
                    )
                }
                if (items.apps.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(emptyMessage(apps)),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    itemsPositioned(
                        items = items.apps,
                        key = { _, item -> item.packageName.value },
                    ) { position, item ->
                        AppRow(
                            packageName = item.packageName,
                            applicationName = item.applicationName,
                            position = position,
                            onClick = { onAction(InventoryExportAction.AppToggled(item.packageName)) },
                            trailingContent = {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { onAction(InventoryExportAction.AppToggled(item.packageName)) },
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionHeader(
    apps: AppSelectionState,
    hasVisibleApps: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.inventory_export_apps_selected, apps.summary.selectedCount, apps.summary.candidateCount),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (hasVisibleApps) {
            TextButton(
                text = stringResource(if (apps.areAllVisibleSelected) R.string.inventory_export_clear else R.string.inventory_export_select_all),
                onClick = onToggleVisible,
            )
        }
    }
}

private fun emptyMessage(apps: AppSelectionState): Int = when {
    apps.summary.candidateCount > 0 -> R.string.inventory_export_no_search_results
    apps.summary.isFilterActive -> R.string.inventory_export_no_apps
    else -> R.string.inventory_export_no_apps_available
}

private val previewItems = listOf(
    InventoryAppItem(PackageName("com.instagram.android"), "Instagram", isSelected = true),
    InventoryAppItem(PackageName("com.whatsapp"), "WhatsApp", isSelected = true),
    InventoryAppItem(PackageName("com.spotify.music"), "Spotify", isSelected = false),
)

@Preview
@Composable
private fun AppsPagePreview() {
    ApkAnalyzerTheme {
        AppsPage(
            state = InventoryExportState(
                showIntro = true,
                apps = AppSelectionState(
                    summary = AppSelectionSummary(isLoaded = true, candidateCount = 3, selectedCount = 2, isFilterActive = true),
                    items = InventoryAppList.Content(previewItems),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AppsPageDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        AppsPage(
            state = InventoryExportState(
                apps = AppSelectionState(
                    summary = AppSelectionSummary(isLoaded = true, candidateCount = 3, selectedCount = 2),
                    items = InventoryAppList.Content(previewItems),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AppsPageNoResultsPreview() {
    ApkAnalyzerTheme {
        AppsPage(
            state = InventoryExportState(
                apps = AppSelectionState(
                    summary = AppSelectionSummary(isLoaded = true, candidateCount = 3, selectedCount = 3),
                    items = InventoryAppList.Content(emptyList()),
                    query = "zzz",
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AppsPageLoadingPreview() {
    ApkAnalyzerTheme {
        AppsPage(
            state = InventoryExportState(),
            onAction = {},
        )
    }
}
