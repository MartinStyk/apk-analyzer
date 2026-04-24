package sk.styk.martin.apkanalyzer.feature.apps.impl.list

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.uilibrary.components.AppIcon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButtonStyle
import sk.styk.martin.apkanalyzer.core.uilibrary.components.InactiveSearchBar
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.card
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeader
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeaderContainer
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.rememberCollapsingHeaderState
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shape
import sk.styk.martin.apkanalyzer.feature.apps.impl.R
import sk.styk.martin.apkanalyzer.feature.apps.impl.appitem.AppListItemRow

@Composable
fun AppsScreen(
    onAppClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppsEvent.NavigateToAppDetail -> onAppClick(event.packageName)
                is AppsEvent.NavigateToSearch -> onSearchClick()
            }
        }
    }

    AppsContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun AppsContent(
    state: AppsState,
    onAction: (AppsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsingState = rememberCollapsingHeaderState()

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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                    onClick = {},
                )
                IconButton(
                    imageVector = ApkAnalyzerIcons.FileUpload,
                    style = IconButtonStyle.Filled,
                    onClick = {},
                )
            }
            ControlRow(
                selectedSort = state.sortType,
                ascending = state.sortAscending,
                onSortSelect = { onAction(AppsAction.SortTypeSelected(it)) },
                onFilterClick = {},
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

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
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
                sortType = state.sortType,
                onAppClicked = { onAction(AppsAction.AppClicked(it)) },
            )
        }
    }
}

private fun LazyListScope.recentsSectionItems(
    recents: RecentsState,
    sortType: SortType,
    onAppClicked: (String) -> Unit,
) {
    when (recents) {
        RecentsState.Loading -> item(key = "recents_skeleton_section") {
            RecentsSkeleton(modifier = Modifier.animateItem())
        }

        is RecentsState.Content -> if (sortType == SortType.Name) {
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
    sortType: SortType,
    onAppClicked: (String) -> Unit,
) {
    item(key = "installed_apps_header") {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.installed_apps),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    when (apps) {
        AppListState.Loading -> itemsPositioned(
            count = 8,
            key = { "app_skeleton_$it" },
        ) { position ->
            AppListItemRowSkeleton(
                position = position,
                modifier = Modifier.animateItem(),
            )
        }

        is AppListState.Content -> itemsPositioned(
            items = apps.apps,
            key = { _, app -> app.packageName },
        ) { position, app ->
            AppListItemRow(
                app = app,
                onClick = { onAppClicked(app.packageName) },
                position = position,
                sortType = sortType,
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
    recentApps: ImmutableList<AppListItem>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    RecentsSection(modifier = modifier) {
        val listState = rememberLazyListState()

        LaunchedEffect(recentApps) {
            listState.animateScrollToItem(0)
        }

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
                key = { it.packageName },
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
            .clip(Shape.CardShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        AppIcon(packageName = app.packageName, size = 56.dp)

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
private fun ControlRow(
    selectedSort: SortType,
    ascending: Boolean,
    onSortSelect: (SortType) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip(
            label = stringResource(R.string.filter),
            onClick = onFilterClick,
            trailingIcon = ApkAnalyzerIcons.Lock,
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(width = 1.dp, height = 24.dp)
                .background(AppTheme.colors.surfaceVariant),
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SortType.entries.forEach { sortType ->
                val isSelected = sortType == selectedSort
                Chip(
                    label = sortType.displayName(),
                    selected = isSelected,
                    trailingIcon = if (isSelected) {
                        if (ascending) ApkAnalyzerIcons.SortAscending else ApkAnalyzerIcons.SortDescending
                    } else {
                        null
                    },
                    onClick = { onSortSelect(sortType) },
                )
            }
        }
    }
}

@Composable
private fun SortType.displayName(): String = when (this) {
    SortType.Name -> stringResource(R.string.sort_name)
    SortType.Size -> stringResource(R.string.sort_size)
    SortType.InstallDate -> stringResource(R.string.sort_install_date)
    SortType.TargetSdk -> stringResource(R.string.sort_target_sdk)
}

@Preview
@Composable
private fun AppsContentReadyPreview() {
    ApkAnalyzerTheme {
        AppsContent(
            state = AppsState(
                apps = AppListState.Content(
                    apps = persistentListOf(
                        AppListItem(
                            packageName = "com.instagram.android",
                            applicationName = "Instagram",
                            targetSdk = 34,
                            apkSize = AppSize(67_108_864),
                            installTime = 0L,
                        ),
                        AppListItem(
                            packageName = "com.whatsapp",
                            applicationName = "WhatsApp",
                            targetSdk = 33,
                            apkSize = AppSize(33_554_432),
                            installTime = 0L,
                        ),
                        AppListItem(
                            packageName = "com.shadowy.apk",
                            applicationName = "Unknown App",
                            targetSdk = 21,
                            apkSize = AppSize(12_582_912),
                            installTime = 0L,
                        ),
                    ),
                ),
                recents = RecentsState.Content(
                    apps = persistentListOf(
                        AppListItem(
                            packageName = "com.instagram.android",
                            applicationName = "Instagram",
                            targetSdk = 34,
                            apkSize = AppSize(67_108_864),
                            installTime = 0L,
                        ),
                        AppListItem(
                            packageName = "com.whatsapp",
                            applicationName = "WhatsApp",
                            targetSdk = 33,
                            apkSize = AppSize(33_554_432),
                            installTime = 0L,
                        ),
                    ),
                ),
            ),
            onAction = {},
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
        )
    }
}
