package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.AppRow
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.AppRowSkeleton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Chip
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.R
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportScaffold
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components.StepContentPadding

private const val SKELETON_COUNT = 8

@Composable
internal fun AppsStepScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    onEditFilters: () -> Unit,
    onAppDetails: (PackageName) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppsStepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AppsStepEvent.NavigateBack -> onBack()
                AppsStepEvent.NavigateToColumns -> onNext()
                AppsStepEvent.NavigateToFilter -> onEditFilters()
                is AppsStepEvent.NavigateToAppDetail -> onAppDetails(event.packageName)
            }
        }
    }

    AppsStepContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun AppsStepContent(
    state: AppsStepState,
    onAction: (AppsStepAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    InventoryExportScaffold(
        currentStep = InventoryExportStep.Apps,
        onBack = { onAction(AppsStepAction.NavigateBack) },
        onSelectStep = {},
        bottomBar = {
            Button(
                text = stringResource(R.string.inventory_export_next),
                onClick = { onAction(AppsStepAction.Continue) },
                enabled = state.canContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = modifier,
    ) {
        AppsList(
            apps = state.apps,
            isFilterActive = state.isFilterActive,
            onEditFilters = { onAction(AppsStepAction.EditFilters) },
            onIncludeAllApps = { onAction(AppsStepAction.IncludeAllApps) },
            onAppClick = { onAction(AppsStepAction.AppClicked(it)) },
        )
    }
}

@Composable
private fun AppsList(
    apps: InventoryAppList,
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
            AppsHeader(
                apps = apps,
                isFilterActive = isFilterActive,
                onEditFilters = onEditFilters,
                onIncludeAllApps = onIncludeAllApps,
            )
        }

        when (apps) {
            InventoryAppList.Loading -> itemsPositioned(
                count = SKELETON_COUNT,
                key = { "app_skeleton_$it" },
            ) { position ->
                AppRowSkeleton(position = position)
            }

            is InventoryAppList.Content -> if (apps.apps.isEmpty()) {
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
                    AppRow(
                        packageName = app.packageName,
                        applicationName = app.applicationName,
                        onClick = { onAppClick(app.packageName) },
                        position = position,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppsHeader(
    apps: InventoryAppList,
    isFilterActive: Boolean,
    onEditFilters: () -> Unit,
    onIncludeAllApps: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = when (apps) {
                InventoryAppList.Loading -> stringResource(R.string.inventory_export_apps_loading)
                is InventoryAppList.Content -> pluralStringResource(R.plurals.inventory_export_app_count, apps.apps.size, apps.apps.size)
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

private val previewApps = listOf(
    InventoryAppItem(PackageName("com.instagram.android"), "Instagram"),
    InventoryAppItem(PackageName("com.whatsapp"), "WhatsApp"),
    InventoryAppItem(PackageName("com.spotify.music"), "Spotify"),
)

@Preview
@Composable
private fun AppsStepContentPreview() {
    ApkAnalyzerTheme {
        AppsStepContent(
            state = AppsStepState(
                apps = InventoryAppList.Content(previewApps),
                isFilterActive = true,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AppsStepLoadingPreview() {
    ApkAnalyzerTheme {
        AppsStepContent(
            state = AppsStepState(),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AppsStepEmptyPreview() {
    ApkAnalyzerTheme {
        AppsStepContent(
            state = AppsStepState(
                apps = InventoryAppList.Content(emptyList()),
                isFilterActive = true,
            ),
            onAction = {},
        )
    }
}
