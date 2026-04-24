package sk.styk.martin.apkanalyzer.feature.apps.impl.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButtonStyle
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shape
import sk.styk.martin.apkanalyzer.feature.apps.impl.R
import sk.styk.martin.apkanalyzer.feature.apps.impl.appitem.AppListItemRow
import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppListItem

@Composable
fun AppSearchScreen(
    onAppClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppSearchEvent.NavigateToAppDetail -> onAppClick(event.packageName)
            }
        }
    }

    AppSearchContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun AppSearchContent(
    state: AppSearchState,
    onAction: (AppSearchAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.background)
                .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        ) {
            IconButton(
                imageVector = ApkAnalyzerIcons.Back,
                onClick = onBack,
            )

            SearchBarActive(
                query = state.query,
                onQueryChange = { onAction(AppSearchAction.QueryChanged(it)) },
                placeholder = stringResource(R.string.search_apps_hint),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
        }

        when {
            state.query.isNotBlank() && state.results.isEmpty() -> NoResultsContent(query = state.query)

            state.query.isNotBlank() -> SearchResults(
                results = state.results,
                onAppClick = { onAction(AppSearchAction.AppClicked(it)) },
            )

            state.searchHistory.isNotEmpty() -> RecentSearchesContent(
                history = state.searchHistory,
                onQueryClick = { onAction(AppSearchAction.HistoryQueryClicked(it)) },
                onDeleteItem = { onAction(AppSearchAction.DeleteHistoryItem(it)) },
                onClearAll = { onAction(AppSearchAction.ClearHistory) },
            )

            else -> EmptySearchContent(totalAppCount = state.totalAppCount)
        }
    }
}

@Composable
private fun SearchResults(results: ImmutableList<AppListItem>, onAppClick: (String) -> Unit) {
    val resultsListState = rememberLazyListState()
    LaunchedEffect(results) {
        resultsListState.scrollToItem(0)
    }
    LazyColumn(
        state = resultsListState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsPositioned(
            items = results,
            key = { _, app -> app.packageName },
        ) { position, app ->
            AppListItemRow(
                app = app,
                onClick = { onAppClick(app.packageName) },
                position = position,
            )
        }
    }
}

@Composable
private fun RecentSearchesContent(
    history: ImmutableList<String>,
    onQueryClick: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "history_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.search_history_title),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.search_history_clear),
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.clickable { onClearAll() },
                )
            }
        }
        items(
            count = history.size,
            key = { history[it] },
        ) { index ->
            SearchHistoryRow(
                query = history[index],
                onClick = { onQueryClick(history[index]) },
                onDelete = { onDeleteItem(history[index]) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
fun SearchHistoryRow(
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shape.CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.History,
            contentDescription = null,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = query,
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            imageVector = ApkAnalyzerIcons.Clear,
            onClick = onDelete,
            modifier = Modifier.size(32.dp),
            style = IconButtonStyle.StandardMuted,
        )
    }
}

@Composable
private fun EmptySearchContent(totalAppCount: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.search_apps_empty, totalAppCount),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoResultsContent(query: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.search_apps_no_results, query),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun AppSearchContentEmptyPreview() {
    ApkAnalyzerTheme {
        AppSearchContent(
            state = AppSearchState(totalAppCount = 342),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AppSearchContentHistoryPreview() {
    ApkAnalyzerTheme {
        AppSearchContent(
            state = AppSearchState(
                totalAppCount = 342,
                searchHistory = persistentListOf(
                    "com.google.android.sdk",
                    "Firebase",
                    "Target SDK 34",
                ),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AppSearchContentResultsPreview() {
    ApkAnalyzerTheme {
        AppSearchContent(
            state = AppSearchState(
                query = "insta",
                results = persistentListOf(
                    AppListItem(
                        packageName = "com.instagram.android",
                        applicationName = "Instagram",
                        targetSdk = 34,
                        apkSize = AppSize(67_108_864),
                        installTime = 0L,
                    ),
                ),
                totalAppCount = 342,
            ),
            onAction = {},
            onBack = {},
        )
    }
}
