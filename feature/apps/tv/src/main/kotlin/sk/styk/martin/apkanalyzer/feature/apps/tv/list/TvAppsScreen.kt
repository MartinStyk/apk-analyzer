package sk.styk.martin.apkanalyzer.feature.apps.tv.list

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.api.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.AppCard
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.AppCardWidth
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.ConfirmDialog
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.SectionHeader
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.SelectableListItem
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.SidePanel
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.RestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.rememberRestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.restorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.bleedHorizontally
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.apps.tv.R
import sk.styk.martin.apkanalyzer.feature.apps.tv.components.TvAppItem
import java.text.DateFormat
import java.time.Instant
import java.util.Date
import sk.styk.martin.apkanalyzer.feature.apps.api.R as AppsApiR

@Composable
internal fun TvAppsScreen(
    onOpenAppDetail: (PackageName) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFilters: () -> Unit,
    viewModel: TvAppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TvAppsEvent.NavigateToAppDetail -> onOpenAppDetail(event.packageName)
                TvAppsEvent.OpenUsageAccessSettings -> context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                TvAppsEvent.NavigateToSearch -> onOpenSearch()
                TvAppsEvent.NavigateToFilters -> onOpenFilters()
            }
        }
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.onAction(TvAppsAction.ScreenResumed)
        onPauseOrDispose { }
    }

    TvAppsContent(state = state, onAction = viewModel::onAction)

    when (state.overlay) {
        TvAppsOverlay.None -> Unit

        TvAppsOverlay.SortPanel -> SortPanel(
            sortOptions = state.sortOptions,
            sortType = state.sortType,
            sortAscending = state.sortAscending,
            onAction = viewModel::onAction,
        )

        TvAppsOverlay.UsageAccessRationale -> ConfirmDialog(
            title = stringResource(R.string.tv_apps_usage_access_title),
            text = stringResource(R.string.tv_apps_usage_access_text),
            confirmLabel = stringResource(R.string.tv_apps_usage_access_open),
            dismissLabel = stringResource(R.string.tv_apps_usage_access_dismiss),
            onConfirm = { viewModel.onAction(TvAppsAction.OpenUsageAccessSettings) },
            onDismiss = { viewModel.onAction(TvAppsAction.OverlayDismissed) },
        )
    }
}

@Composable
private fun SortPanel(
    sortOptions: List<AppSortType>,
    sortType: AppSortType,
    sortAscending: Boolean,
    onAction: (TvAppsAction) -> Unit,
) {
    val selectedFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        selectedFocusRequester.requestFocus()
    }

    SidePanel(
        title = stringResource(R.string.tv_apps_sort_title),
        onDismissRequest = { onAction(TvAppsAction.OverlayDismissed) },
    ) {
        sortOptions.forEach { option ->
            val selected = option == sortType
            SelectableListItem(
                selected = selected,
                onClick = { onAction(TvAppsAction.SortTypeSelected(option)) },
                label = option.label(),
                supportingLabel = if (selected) option.orderLabel(sortAscending) else null,
                selectedIcon = if (sortAscending) ApkAnalyzerIcons.SortAscending else ApkAnalyzerIcons.SortDescending,
                modifier = if (selected) Modifier.focusRequester(selectedFocusRequester) else Modifier,
            )
        }
    }
}

@Composable
private fun AppSortType.orderLabel(ascending: Boolean): String = stringResource(
    when (this) {
        AppSortType.Name -> if (ascending) R.string.tv_apps_order_a_to_z else R.string.tv_apps_order_z_to_a

        AppSortType.TotalSize, AppSortType.ApkSize ->
            if (ascending) R.string.tv_apps_order_smallest_first else R.string.tv_apps_order_largest_first

        AppSortType.InstallDate, AppSortType.LastUpdated, AppSortType.LastUsed ->
            if (ascending) R.string.tv_apps_order_oldest_first else R.string.tv_apps_order_newest_first

        AppSortType.TargetSdk -> if (ascending) R.string.tv_apps_order_lowest_first else R.string.tv_apps_order_highest_first
    },
)

@Composable
private fun TvAppsContent(state: TvAppsState, onAction: (TvAppsAction) -> Unit) {
    when (val apps = state.apps) {
        TvAppListState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.tv_apps_loading), color = AppTheme.colors.onSurfaceVariant)
        }

        is TvAppListState.Content -> {
            val cardFocus = rememberRestorableFocus(
                availableKeys = buildSet {
                    add(SEARCH_KEY)
                    add(SORT_KEY)
                    add(FILTERS_KEY)
                    if (state.activeFilterCount > 0) add(CLEAR_FILTERS_KEY)
                    state.recents.forEach { add(recentCardKey(it.packageName)) }
                    apps.apps.forEach { add(installedCardKey(it.packageName)) }
                },
                defaultKey = state.recents.firstOrNull()?.let { recentCardKey(it.packageName) }
                    ?: apps.apps.firstOrNull()?.let { installedCardKey(it.packageName) }
                    ?: SEARCH_KEY,
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 48.dp, top = 27.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(AppsApiR.string.apps), style = AppTheme.typography.headlineSmall)
                        Text(
                            text = pluralStringResource(R.plurals.tv_apps_count, apps.apps.size, apps.apps.size),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { onAction(TvAppsAction.SearchClicked) },
                        label = stringResource(R.string.tv_apps_search),
                        leadingIcon = ApkAnalyzerIcons.Search,
                        modifier = Modifier.restorableFocus(cardFocus, SEARCH_KEY),
                    )
                    Button(
                        onClick = { onAction(TvAppsAction.SortClicked) },
                        label = stringResource(R.string.tv_apps_sort_button, state.sortType.label()),
                        leadingIcon = if (state.sortAscending) ApkAnalyzerIcons.SortAscending else ApkAnalyzerIcons.SortDescending,
                        modifier = Modifier.restorableFocus(cardFocus, SORT_KEY),
                    )
                    Button(
                        onClick = { onAction(TvAppsAction.FiltersClicked) },
                        label = if (state.activeFilterCount > 0) {
                            stringResource(R.string.tv_apps_filters_active, state.activeFilterCount)
                        } else {
                            stringResource(R.string.tv_apps_filters)
                        },
                        leadingIcon = ApkAnalyzerIcons.Filter,
                        modifier = Modifier.restorableFocus(cardFocus, FILTERS_KEY),
                    )
                    if (state.activeFilterCount > 0) {
                        Button(
                            onClick = { onAction(TvAppsAction.ClearFilters) },
                            label = stringResource(R.string.tv_apps_clear_filters),
                            modifier = Modifier.restorableFocus(cardFocus, CLEAR_FILTERS_KEY),
                        )
                    }
                }
                AppGrid(state = state, apps = apps.apps, cardFocus = cardFocus, onAction = onAction)
            }
        }
    }
}

@Composable
private fun AppGrid(
    state: TvAppsState,
    apps: List<TvAppItem>,
    cardFocus: RestorableFocus,
    onAction: (TvAppsAction) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = AppCardWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 12.dp, bottom = 27.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.recents.isNotEmpty()) {
            item(key = RECENTS_KEY, span = { GridItemSpan(maxLineSpan) }) {
                RecentsSection(recents = state.recents, cardFocus = cardFocus, onAction = onAction)
            }
        }
        item(key = INSTALLED_HEADER_KEY, span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle(text = stringResource(R.string.tv_apps_installed))
        }
        if (apps.isEmpty() && state.activeFilterCount > 0) {
            item(key = NO_MATCH_KEY, span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.tv_apps_no_filter_match),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
        }
        items(items = apps, key = { it.packageName.value }) { app ->
            AppCard(
                packageName = app.packageName,
                applicationName = app.applicationName,
                onClick = { onAction(TvAppsAction.AppClicked(app.packageName)) },
                description = app.sortValue(state.sortType),
                modifier = Modifier.restorableFocus(cardFocus, installedCardKey(app.packageName)),
            )
        }
    }
}

@Composable
private fun RecentsSection(
    recents: List<TvAppItem>,
    cardFocus: RestorableFocus,
    onAction: (TvAppsAction) -> Unit,
) {
    val rowState = rememberLazyListState()
    LaunchedEffect(recents.firstOrNull()?.packageName) {
        rowState.scrollToItem(0)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(text = stringResource(R.string.tv_apps_recently_viewed))
        LazyRow(
            state = rowState,
            modifier = Modifier.bleedHorizontally(FocusBleed),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = FocusBleed, vertical = 12.dp),
        ) {
            items(items = recents, key = { it.packageName.value }) { app ->
                AppCard(
                    packageName = app.packageName,
                    applicationName = app.applicationName,
                    onClick = { onAction(TvAppsAction.AppClicked(app.packageName)) },
                    modifier = Modifier.restorableFocus(cardFocus, recentCardKey(app.packageName)).width(AppCardWidth),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    SectionHeader(text = text, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun AppSortType.label(): String = when (this) {
    AppSortType.Name -> stringResource(R.string.tv_apps_sort_name)
    AppSortType.ApkSize -> stringResource(R.string.tv_apps_sort_apk_size)
    AppSortType.TotalSize -> stringResource(R.string.tv_apps_sort_total_size)
    AppSortType.InstallDate -> stringResource(R.string.tv_apps_sort_install_date)
    AppSortType.LastUpdated -> stringResource(R.string.tv_apps_sort_last_updated)
    AppSortType.LastUsed -> stringResource(R.string.tv_apps_sort_last_used)
    AppSortType.TargetSdk -> stringResource(R.string.tv_apps_sort_target_sdk)
}

@Composable
private fun TvAppItem.sortValue(sortType: AppSortType): String? = when (sortType) {
    AppSortType.Name -> null

    AppSortType.ApkSize -> apkSize.formatted()

    AppSortType.TotalSize -> totalSize?.formatted() ?: stringResource(R.string.tv_apps_value_size_unknown)

    AppSortType.InstallDate -> if (installTime.isPreinstalled()) {
        stringResource(R.string.tv_apps_value_preinstalled)
    } else {
        stringResource(R.string.tv_apps_value_installed, installTime.toMediumDate())
    }

    AppSortType.LastUpdated -> stringResource(R.string.tv_apps_value_updated, lastUpdateTime.toMediumDate())

    AppSortType.LastUsed ->
        lastUsedTime
            ?.let { stringResource(R.string.tv_apps_value_last_used, it.toMediumDate()) }
            ?: stringResource(R.string.tv_apps_value_never_used)

    AppSortType.TargetSdk -> stringResource(R.string.tv_apps_value_target_sdk, targetSdk)
}

private fun Instant.isPreinstalled(): Boolean = this < PreinstalledCutoff

@Composable
private fun Instant.toMediumDate(): String = DateFormat
    .getDateInstance(DateFormat.MEDIUM, LocaleList.current[0].platformLocale)
    .format(Date(toEpochMilli()))

private fun installedCardKey(packageName: PackageName) = "installed:${packageName.value}"

private fun recentCardKey(packageName: PackageName) = "recent:${packageName.value}"

private val FocusBleed = 16.dp
private val PreinstalledCutoff: Instant = Instant.parse("1990-01-01T00:00:00Z")
private const val SEARCH_KEY = "search"
private const val SORT_KEY = "sort"
private const val FILTERS_KEY = "filters"
private const val CLEAR_FILTERS_KEY = "clear-filters"
private const val NO_MATCH_KEY = "no-match"
private const val RECENTS_KEY = "recents"
private const val INSTALLED_HEADER_KEY = "installed-header"

private val previewApps = listOf(
    TvAppItem(
        packageName = PackageName("com.google.android.youtube.tv"),
        applicationName = "YouTube",
        targetSdk = 34,
        apkSize = 142.megabytes,
        totalSize = 380.megabytes,
        installTime = Instant.parse("2025-03-02T10:00:00Z"),
        lastUpdateTime = Instant.parse("2026-09-14T10:00:00Z"),
        lastUsedTime = Instant.parse("2026-10-20T19:00:00Z"),
    ),
    TvAppItem(
        packageName = PackageName("com.netflix.ninja"),
        applicationName = "Netflix",
        targetSdk = 35,
        apkSize = 98.megabytes,
        totalSize = null,
        installTime = Instant.parse("2025-03-02T10:00:00Z"),
        lastUpdateTime = Instant.parse("2026-10-01T10:00:00Z"),
        lastUsedTime = null,
    ),
)

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvAppsContentPreview() {
    ApkAnalyzerTvTheme {
        TvAppsContent(
            state = TvAppsState(
                apps = TvAppListState.Content(previewApps),
                recents = previewApps.take(1),
                sortOptions = AppSortType.entries,
            ),
            onAction = {},
        )
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvAppsSortedPreview() {
    ApkAnalyzerTvTheme {
        TvAppsContent(
            state = TvAppsState(
                apps = TvAppListState.Content(previewApps),
                sortType = AppSortType.ApkSize,
                sortAscending = false,
                sortOptions = AppSortType.entries,
            ),
            onAction = {},
        )
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvAppsLoadingPreview() {
    ApkAnalyzerTvTheme {
        TvAppsContent(state = TvAppsState(), onAction = {})
    }
}
