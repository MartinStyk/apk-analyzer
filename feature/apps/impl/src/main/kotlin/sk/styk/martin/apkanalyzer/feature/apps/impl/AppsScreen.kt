package sk.styk.martin.apkanalyzer.feature.apps.impl

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.uilibrary.animation.AnimatedExpansion
import sk.styk.martin.apkanalyzer.core.uilibrary.components.AppIcon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBar
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.card
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeader
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeaderContainer
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.rememberCollapsingHeaderState
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppShapes
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun AppsScreen(
    onAppClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AppsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppsEvent.NavigateToAppDetail -> onAppClicked(event.packageName)
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
    when (state) {
        is AppsState.Loading -> LoadingContent(modifier)
        is AppsState.Ready -> ReadyContent(state, onAction, modifier)
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingSpinner()
    }
}

@Composable
private fun ReadyContent(
    state: AppsState.Ready,
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
            SearchBar(
                query = state.searchQuery,
                onQueryChanged = { onAction(AppsAction.SearchQueryChanged(it)) },
                placeholder = stringResource(R.string.search_apps_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            ControlRow(
                selectedSort = state.sortType,
                ascending = state.sortAscending,
                onSortSelected = { onAction(AppsAction.SortTypeSelected(it)) },
                onFilterClicked = { /* TODO premium filter bottom sheet */ },
            )

            AnimatedExpansion(visible = state.recentApps.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.recently_viewed),
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colors.onBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RecentAppsRow(
                        recentApps = state.recentApps,
                        onAppClicked = { onAction(AppsAction.AppClicked(it)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.installed_apps),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset { collapsingState.contentOffset }
                .padding(bottom = 16.dp)
                .card(),
        ) {
            items(
                items = state.apps,
                key = { it.packageName },
            ) { app ->
                AppListItemRow(
                    app = app,
                    onClick = { onAction(AppsAction.AppClicked(app.packageName)) },
                )
            }
        }

    }
}

@Composable
private fun RecentAppsRow(
    recentApps: ImmutableList<AppListItem>,
    onAppClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(recentApps) {
        listState.animateScrollToItem(0)
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .card(),
    ) {
        items(
            items = recentApps,
            key = { it.packageName },
        ) { app ->
            RecentAppItem(
                app = app,
                onClick = { onAppClicked(app.packageName) },
            )
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
            .clip(AppShapes.CardShape)
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
    onSortSelected: (SortType) -> Unit,
    onFilterClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip(
            label = stringResource(R.string.filter),
            onClick = onFilterClicked,
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
                    onClick = { onSortSelected(sortType) },
                )
            }
        }
    }
}

@Composable
private fun AppListItemRow(
    app: AppListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.packageName)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.applicationName,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = app.packageName,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = app.apkSize.formatted(),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                )
                app.versionName?.let { version ->
                    Text(
                        text = "•",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                    )

                    Text(
                        text = version,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TargetSdkBadge(targetSdk = app.targetSdk)
            SourceBadge(source = app.source)
        }
    }
}

@Composable
private fun TargetSdkBadge(targetSdk: Int, modifier: Modifier = Modifier) {
    val (textColor, bgColor) = when {
        targetSdk >= 34 -> AppTheme.colors.positive to AppTheme.colors.positiveContainer
        targetSdk >= 30 -> AppTheme.colors.warning to AppTheme.colors.warningContainer
        else -> AppTheme.colors.negative to AppTheme.colors.negativeContainer
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = targetSdk.toString(),
            style = AppTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

@Composable
private fun SourceBadge(source: AppSource, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val (label, textColor, bgColor) = when (source) {
        AppSource.GooglePlay -> Triple(stringResource(R.string.source_google_play), colors.positive, colors.positiveContainer)
        AppSource.AmazonStore -> Triple(stringResource(R.string.source_amazon_store), colors.primary, colors.primaryContainer)
        AppSource.SystemPreinstalled -> Triple(stringResource(R.string.source_system), colors.onSurfaceVariant, colors.surfaceVariant)
        AppSource.Unknown -> Triple(stringResource(R.string.source_unknown), colors.warning, colors.warningContainer)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelSmall,
            color = textColor,
        )
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
            state = AppsState.Ready(
                apps = persistentListOf(
                    AppListItem(
                        packageName = "com.instagram.android",
                        applicationName = "Instagram",
                        targetSdk = 34,
                        apkSize = AppSize(67_108_864),
                        source = AppSource.GooglePlay,
                        versionName = "312.0.0",
                        installTime = 0L,
                    ),
                    AppListItem(
                        packageName = "com.whatsapp",
                        applicationName = "WhatsApp",
                        targetSdk = 33,
                        apkSize = AppSize(33_554_432),
                        source = AppSource.GooglePlay,
                        versionName = "2.24.1",
                        installTime = 0L,
                    ),
                    AppListItem(
                        packageName = "com.shadowy.apk",
                        applicationName = "Unknown App",
                        targetSdk = 21,
                        apkSize = AppSize(12_582_912),
                        source = AppSource.Unknown,
                        versionName = "1.0",
                        installTime = 0L,
                    ),
                ),
                totalAppCount = 342,
                recentApps = persistentListOf(
                    AppListItem(
                        packageName = "com.instagram.android",
                        applicationName = "Instagram",
                        targetSdk = 34,
                        apkSize = AppSize(67_108_864),
                        source = AppSource.GooglePlay,
                        versionName = "312.0.0",
                        installTime = 0L,
                    ),
                    AppListItem(
                        packageName = "com.whatsapp",
                        applicationName = "WhatsApp",
                        targetSdk = 33,
                        apkSize = AppSize(33_554_432),
                        source = AppSource.GooglePlay,
                        versionName = "2.24.1",
                        installTime = 0L,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
