package sk.styk.martin.apkanalyzer.feature.browse.impl.apps

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.uilibrary.components.AppIcon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeader
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeaderContainer
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.rememberCollapsingHeaderState
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.browse.impl.R
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

@Composable
internal fun BrowseAppsScreen(
    dimension: BrowseDimension,
    bucketKey: String,
    bucketLabel: String,
    subAttribute: String?,
    onBack: () -> Unit,
    onOpenApp: (PackageName) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseAppsViewModel = hiltViewModel { factory: BrowseAppsViewModel.Factory ->
        factory.create(dimension, bucketKey, subAttribute)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowseAppsEvent.NavigateToAppDetail -> onOpenApp(event.packageName)
            }
        }
    }

    BrowseAppsContent(
        bucketLabel = bucketLabel,
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun BrowseAppsContent(
    bucketLabel: String,
    state: BrowseAppsState,
    onAction: (BrowseAppsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(title = bucketLabel, onBack = onBack)

        when (state) {
            is BrowseAppsState.Loading -> Box(modifier = Modifier.fillMaxSize()) {
                LoadingSpinner(modifier = Modifier.align(Alignment.Center))
            }

            is BrowseAppsState.Loaded -> BrowseAppsList(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun BrowseAppsList(
    state: BrowseAppsState.Loaded,
    onAction: (BrowseAppsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsingState = rememberCollapsingHeaderState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .collapsingHeaderContainer(collapsingState),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { collapsingState.headerOffset }
                .background(AppTheme.colors.background)
                .collapsingHeader(collapsingState),
        ) {
            SearchBarActive(
                query = state.query,
                placeholder = pluralStringResource(R.plurals.browse_apps_filter_hint, state.totalApps, state.totalApps),
                onQueryChange = { onAction(BrowseAppsAction.ChangeQuery(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (state.apps.isNotEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { collapsingState.contentOffset },
            ) {
                items(items = state.apps, key = { it.packageName.value }) { app ->
                    BrowseAppRow(
                        app = app,
                        onClick = { onAction(BrowseAppsAction.AppClicked(app.packageName)) },
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.browse_apps_empty_query, state.query),
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { collapsingState.contentOffset }
                    .padding(horizontal = 32.dp, vertical = 48.dp),
            )
        }
    }
}

@Composable
private fun BrowseAppRow(
    app: BrowseAppItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        AppIcon(source = AppReference.InstalledPackage(app.packageName))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.applicationName,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = app.packageName.value,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun BrowseAppsLoadingPreview() {
    ApkAnalyzerTheme {
        BrowseAppsContent(bucketLabel = "Camera", state = BrowseAppsState.Loading, onAction = {}, onBack = {})
    }
}

@Preview
@Composable
private fun BrowseAppsLoadedPreview() {
    ApkAnalyzerTheme {
        BrowseAppsContent(bucketLabel = "Camera", state = sampleLoadedState(), onAction = {}, onBack = {})
    }
}

@Preview
@Composable
private fun BrowseAppsEmptyPreview() {
    ApkAnalyzerTheme {
        BrowseAppsContent(
            bucketLabel = "Camera",
            state = sampleLoadedState().copy(query = "xyz", apps = persistentListOf()),
            onAction = {},
            onBack = {},
        )
    }
}

private fun sampleLoadedState() = BrowseAppsState.Loaded(
    query = "",
    totalApps = 2,
    apps = persistentListOf(
        BrowseAppItem(packageName = PackageName("com.instagram.android"), applicationName = "Instagram"),
        BrowseAppItem(packageName = PackageName("com.spotify.music"), applicationName = "Spotify"),
    ).toImmutableList(),
)
