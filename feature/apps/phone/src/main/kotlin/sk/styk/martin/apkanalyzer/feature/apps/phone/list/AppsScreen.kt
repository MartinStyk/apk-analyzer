package sk.styk.martin.apkanalyzer.feature.apps.phone.list

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.AppRowSkeleton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Chip
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ChipVariant
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.IconButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.IconButtonStyle
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.InactiveSearchBar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.navigationBarContentPadding
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.ShimmerGroup
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.card
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.collapsingHeader
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.collapsingHeaderContainer
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.rememberCollapsingHeaderState
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.sharedBounds
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.apps.phone.R
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.AppDataPermission
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.PermissionRationaleBottomSheet
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.RecentAppsRowSkeleton
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.apkfilepicker.ApkFilePickerButton
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.appitem.AppListItemRow
import sk.styk.martin.apkanalyzer.feature.apps.phone.components.quickfilter.QuickFilterRow
import java.time.Instant

private const val APP_LIST_ITEM_TAG = "app_list_item_row"

@Composable
internal fun AppsScreen(
    onAppDetails: (PackageName) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onApkDetails: (String) -> Unit,
    onFilter: () -> Unit,
    onInventoryExport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppsEvent.NavigateToAppDetail -> onAppDetails(event.packageName)
                is AppsEvent.NavigateToSearch -> onSearch()
                is AppsEvent.NavigateToSettings -> onSettings()
                is AppsEvent.NavigateToFilter -> onFilter()
                is AppsEvent.NavigateToInventoryExport -> onInventoryExport()
                is AppsEvent.OpenUsageAccessSettings -> context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
    }

    AppsContent(
        state = state,
        onAction = viewModel::onAction,
        onApkDetails = onApkDetails,
        modifier = modifier,
    )
}

@Composable
private fun AppsContent(
    state: AppsState,
    onAction: (AppsAction) -> Unit,
    onApkDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSortSheet by remember { mutableStateOf(false) }
    val collapsingState = rememberCollapsingHeaderState()

    LaunchedEffect(state.sortPermissionRationale) {
        if (state.sortPermissionRationale != null) showSortSheet = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .collapsingHeaderContainer(collapsingState),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { collapsingState.headerOffset }
                .background(AppTheme.colors.background)
                .collapsingHeader(collapsingState),
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InactiveSearchBar(
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(R.string.search_apps_hint),
                    onClick = { onAction(AppsAction.SearchClicked) },
                )
                IconButton(
                    imageVector = ApkAnalyzerIcons.Settings,
                    style = IconButtonStyle.Filled,
                    onClick = { onAction(AppsAction.OpenSettings) },
                    modifier = Modifier.sharedBounds("settings"),
                    contentDescription = stringResource(R.string.content_description_settings),
                )
                ApkFilePickerButton(onApkDetails = onApkDetails)
            }
            QuickFilterRow(
                onFilter = { onAction(AppsAction.FilterClicked) },
            )
        }

        val lazyListState = rememberLazyListState()
        var skipFirstScroll by remember { mutableStateOf(true) }

        LaunchedEffect(state.sortType, state.sortAscending) {
            if (skipFirstScroll) {
                skipFirstScroll = false
                return@LaunchedEffect
            }
            lazyListState.scrollToItem(0)
        }

        ShimmerGroup {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = navigationBarContentPadding + 16.dp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { collapsingState.contentOffset },
            ) {
                recentsSectionItems(
                    recents = state.recents,
                    sortType = state.sortType,
                    onAppClicked = { onAction(AppsAction.AppClicked(it)) },
                )

                appsSectionItems(
                    apps = state.apps,
                    isFiltering = state.activeFilter.isActive,
                    isInventoryExportEnabled = state.isInventoryExportEnabled,
                    sortType = state.sortType,
                    onAppClicked = { onAction(AppsAction.AppClicked(it)) },
                    onClearFilters = { onAction(AppsAction.ClearAllFilters) },
                    onShowSort = { showSortSheet = true },
                    onExport = { onAction(AppsAction.ExportClicked) },
                )
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            sortType = state.sortType,
            sortAscending = state.sortAscending,
            onSortSelect = { onAction(AppsAction.SortTypeSelected(it)) },
            onDismiss = { showSortSheet = false },
        )
    }

    state.sortPermissionRationale?.let { rationale ->
        PermissionRationaleBottomSheet(
            title = stringResource(rationale.sortTitleRes()),
            description = stringResource(rationale.sortDescriptionRes()),
            openSettingsLabel = stringResource(R.string.quick_filter_permission_open_settings),
            onOpenSettings = { onAction(AppsAction.OpenSortPermissionSettings(rationale)) },
            onDismiss = { onAction(AppsAction.DismissSortPermissionRationale) },
        )
    }
}

private fun LazyListScope.recentsSectionItems(
    recents: RecentsState,
    sortType: AppSortType,
    onAppClicked: (PackageName) -> Unit,
) {
    when (recents) {
        RecentsState.Loading -> item(key = "recents_skeleton_section") {
            RecentsSkeleton(modifier = Modifier.animateItem())
        }

        is RecentsState.Content -> if (sortType == AppSortType.Name) {
            item(key = "recents_section") {
                RecentsContent(
                    recentApps = recents.apps,
                    onAppClick = onAppClicked,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        RecentsState.NoRecents -> Unit
    }
}

private fun LazyListScope.appsSectionItems(
    apps: AppListState,
    isFiltering: Boolean,
    isInventoryExportEnabled: Boolean,
    sortType: AppSortType,
    onAppClicked: (PackageName) -> Unit,
    onClearFilters: () -> Unit,
    onShowSort: () -> Unit,
    onExport: () -> Unit,
) {
    item(key = "installed_apps_header") {
        Column {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.installed_apps),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isInventoryExportEnabled) {
                    val exportCount = (apps as? AppListState.Content)?.apps?.size?.takeIf { isFiltering }
                    Chip(
                        label = if (exportCount != null) {
                            pluralStringResource(R.plurals.export_apps_count, exportCount, exportCount)
                        } else {
                            stringResource(R.string.export_apps)
                        },
                        variant = ChipVariant.Default,
                        leadingIcon = ApkAnalyzerIcons.Export,
                        onClick = onExport,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                IconButton(
                    imageVector = ApkAnalyzerIcons.Sort,
                    style = if (sortType == AppSortType.Name) IconButtonStyle.Standard else IconButtonStyle.Highlighted,
                    onClick = onShowSort,
                    modifier = Modifier.size(22.dp),
                    contentDescription = stringResource(R.string.content_description_sort),
                )
            }
        }
    }

    when (apps) {
        AppListState.Loading -> itemsPositioned(
            count = 8,
            key = { "app_skeleton_$it" },
        ) { position ->
            AppRowSkeleton(
                position = position,
                modifier = Modifier.animateItem(),
            )
        }

        is AppListState.Content -> {
            if (apps.apps.isEmpty() && isFiltering) {
                item(key = "empty_filtered") {
                    EmptyFilteredState(
                        onClearFilters = onClearFilters,
                        modifier = Modifier.animateItem(),
                    )
                }
            } else {
                itemsPositioned(
                    items = apps.apps,
                    key = { _, app -> app.packageName.value },
                ) { position, app ->
                    AppListItemRow(
                        app = app,
                        onClick = { onAppClicked(app.packageName) },
                        position = position,
                        sortType = sortType,
                        modifier = Modifier.testTag(APP_LIST_ITEM_TAG),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFilteredState(onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.Search,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.apps_empty_filtered),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Chip(
            label = stringResource(R.string.apps_clear_filters),
            onClick = onClearFilters,
        )
    }
}

@Composable
private fun SortBottomSheet(
    sortType: AppSortType,
    sortAscending: Boolean,
    onSortSelect: (AppSortType) -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.sort_bottom_sheet_title),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            AppSortType.entries.forEach { type ->
                SortOptionRow(
                    type = type,
                    isSelected = sortType == type,
                    ascending = sortAscending,
                    onClick = { onSortSelect(type) },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SortOptionRow(
    type: AppSortType,
    isSelected: Boolean,
    ascending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Text(
            text = type.displayName(),
            style = AppTheme.typography.bodyLarge,
            color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = if (ascending) ApkAnalyzerIcons.SortAscending else ApkAnalyzerIcons.SortDescending,
                tint = AppTheme.colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RecentsSection(modifier: Modifier = Modifier, recentContent: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.recently_viewed),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        recentContent()
    }
}

@Composable
private fun RecentsSkeleton(modifier: Modifier = Modifier) {
    RecentsSection(modifier = modifier) {
        RecentAppsRowSkeleton()
    }
}

@Composable
private fun RecentsContent(
    recentApps: List<AppListItem>,
    onAppClick: (PackageName) -> Unit,
    modifier: Modifier = Modifier,
) {
    RecentsSection(modifier = modifier) {
        val listState = rememberLazyListState()
        LaunchedEffect(recentApps) { listState.animateScrollToItem(0) }

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .card(),
        ) {
            items(
                items = recentApps,
                key = { it.packageName.value },
            ) { app ->
                RecentAppItem(
                    app = app,
                    onClick = { onAppClick(app.packageName) },
                )
            }
        }
    }
}

@Composable
private fun RecentAppItem(
    app: AppListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(80.dp)
            .clip(Shapes.CardShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        AppIcon(
            source = AppReference.InstalledPackage(app.packageName),
            size = 56.dp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.applicationName,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppSortType.displayName(): String = when (this) {
    AppSortType.Name -> stringResource(R.string.sort_name)
    AppSortType.ApkSize -> stringResource(R.string.sort_size)
    AppSortType.TotalSize -> stringResource(R.string.sort_total_size)
    AppSortType.InstallDate -> stringResource(R.string.sort_install_date)
    AppSortType.TargetSdk -> stringResource(R.string.sort_target_sdk)
    AppSortType.LastUpdated -> stringResource(R.string.sort_last_updated)
    AppSortType.LastUsed -> stringResource(R.string.sort_last_used)
}

private fun AppDataPermission.sortTitleRes(): Int = when (this) {
    AppDataPermission.UsageAccess -> R.string.sort_permission_usage_title
    AppDataPermission.StorageAccess -> R.string.sort_permission_storage_title
}

private fun AppDataPermission.sortDescriptionRes(): Int = when (this) {
    AppDataPermission.UsageAccess -> R.string.sort_permission_usage_description
    AppDataPermission.StorageAccess -> R.string.sort_permission_storage_description
}

@Preview
@Composable
private fun AppsContentReadyPreview() {
    ApkAnalyzerTheme {
        AppsContent(
            state = AppsState(
                apps = AppListState.Content(
                    apps = listOf(
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
                    ),
                ),
                recents = RecentsState.NoRecents,
                isInventoryExportEnabled = true,
            ),
            onAction = {},
            onApkDetails = {},
        )
    }
}

@Preview
@Composable
private fun AppsContentExportDisabledPreview() {
    ApkAnalyzerTheme {
        AppsContent(
            state = AppsState(
                apps = AppListState.Content(
                    apps = listOf(
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
                    ),
                ),
                recents = RecentsState.NoRecents,
                isInventoryExportEnabled = false,
            ),
            onAction = {},
            onApkDetails = {},
        )
    }
}

@Preview
@Composable
private fun AppsContentEmptyFilteredPreview() {
    ApkAnalyzerTheme {
        AppsContent(
            state = AppsState(
                apps = AppListState.Content(apps = listOf()),
                recents = RecentsState.NoRecents,
            ),
            onAction = {},
            onApkDetails = {},
        )
    }
}

@Preview
@Composable
private fun AppsContentLoadingPreview() {
    ApkAnalyzerTheme {
        AppsContent(
            state = AppsState(),
            onAction = {},
            onApkDetails = {},
        )
    }
}

@Preview
@Composable
private fun AppsContentActiveFiltersPreview() {
    ApkAnalyzerTheme {
        AppsContent(
            state = AppsState(
                apps = AppListState.Content(apps = listOf()),
                recents = RecentsState.NoRecents,
            ),
            onAction = {},
            onApkDetails = {},
        )
    }
}
