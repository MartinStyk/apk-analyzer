package sk.styk.martin.apkanalyzer.feature.browse.tv.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseBucketDetail
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.core.appindex.browse.cardLabelRes
import sk.styk.martin.apkanalyzer.core.appindex.browse.isCertificateHash
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.AppCard
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.AppCardWidth
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.FingerprintBox
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.ScrollablePanel
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.rememberRestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.restorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.browse.tv.R
import sk.styk.martin.apkanalyzer.feature.browse.tv.bucketIcon
import sk.styk.martin.apkanalyzer.feature.browse.tv.components.BrowseHeader
import sk.styk.martin.apkanalyzer.feature.browse.tv.components.BrowseLoading
import sk.styk.martin.apkanalyzer.feature.browse.tv.components.BrowseSearch
import sk.styk.martin.apkanalyzer.feature.browse.tv.components.BucketDetailInfo
import sk.styk.martin.apkanalyzer.core.appindex.R as AppIndexR

@Composable
internal fun TvBrowseAppsScreen(
    dimension: BrowseDimension,
    bucketKey: String,
    bucketLabel: String,
    subAttribute: BrowseSubAttribute?,
    onOpenApp: (PackageName) -> Unit,
    viewModel: TvBrowseAppsViewModel = hiltViewModel { factory: TvBrowseAppsViewModel.Factory ->
        factory.create(dimension, bucketKey, subAttribute)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TvBrowseAppsEvent.NavigateToAppDetail -> onOpenApp(event.packageName)
            }
        }
    }

    TvBrowseAppsContent(
        dimension = dimension,
        bucketKey = bucketKey,
        bucketLabel = (state as? TvBrowseAppsState.Loaded)?.bucketLabel?.ifEmpty { bucketLabel } ?: bucketLabel,
        subAttribute = subAttribute,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun TvBrowseAppsContent(
    dimension: BrowseDimension,
    bucketKey: String,
    bucketLabel: String,
    subAttribute: BrowseSubAttribute?,
    state: TvBrowseAppsState,
    onAction: (TvBrowseAppsAction) -> Unit,
) {
    when (state) {
        TvBrowseAppsState.Loading -> BrowseLoading()

        is TvBrowseAppsState.Loaded -> LoadedContent(
            dimension = dimension,
            bucketKey = bucketKey,
            bucketLabel = bucketLabel,
            subAttribute = subAttribute,
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun LoadedContent(
    dimension: BrowseDimension,
    bucketKey: String,
    bucketLabel: String,
    subAttribute: BrowseSubAttribute?,
    state: TvBrowseAppsState.Loaded,
    onAction: (TvBrowseAppsAction) -> Unit,
) {
    val focus = rememberRestorableFocus(
        availableKeys = buildSet {
            add(SEARCH_KEY)
            state.apps.forEach { add(it.packageName.value) }
        },
        defaultKey = state.apps.firstOrNull()?.packageName?.value ?: SEARCH_KEY,
    )
    val isHashBucket = subAttribute.isCertificateHash

    Column(modifier = Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, top = 27.dp)) {
        BrowseHeader(
            icon = dimension.bucketIcon(bucketKey),
            title = when {
                !isHashBucket -> bucketLabel

                else -> (state.bucketDetail as? BrowseBucketDetail.Certificate)?.subject?.displayName
                    ?: stringResource(AppIndexR.string.browse_signer_unknown)
            },
            subtitle = pluralStringResource(AppIndexR.plurals.browse_apps_count, state.totalApps, state.totalApps),
        ) {
            BrowseSearch(
                query = state.query,
                onQueryChange = { onAction(TvBrowseAppsAction.ChangeQuery(it)) },
                placeholder = pluralStringResource(AppIndexR.plurals.browse_apps_filter_hint, state.totalApps, state.totalApps),
                modifier = Modifier.restorableFocus(focus, SEARCH_KEY),
            )
        }
        Row(modifier = Modifier.padding(top = 8.dp, bottom = 15.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (state.bucketDetail != null || isHashBucket) {
                BucketInfo(
                    detail = state.bucketDetail,
                    fingerprint = bucketKey.takeIf { isHashBucket },
                    fingerprintLabel = subAttribute?.let { stringResource(it.cardLabelRes) },
                    modifier = Modifier.padding(vertical = FocusRoom).width(InfoWidth),
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = AppCardWidth),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(all = FocusRoom),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (state.apps.isEmpty()) {
                    item(key = EMPTY_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = if (state.query.isBlank()) {
                                stringResource(R.string.tv_browse_no_apps)
                            } else {
                                stringResource(AppIndexR.string.browse_apps_empty_query, state.query)
                            },
                            style = AppTheme.typography.titleMedium,
                            color = AppTheme.colors.onSurfaceVariant,
                        )
                    }
                }
                items(state.apps, key = { it.packageName.value }) { app ->
                    AppCard(
                        packageName = app.packageName,
                        applicationName = app.applicationName,
                        onClick = { onAction(TvBrowseAppsAction.SelectApp(app.packageName)) },
                        modifier = Modifier.restorableFocus(focus, app.packageName.value),
                    )
                }
            }
        }
    }
}

@Composable
private fun BucketInfo(
    detail: BrowseBucketDetail?,
    fingerprint: String?,
    fingerprintLabel: String?,
    modifier: Modifier = Modifier,
) {
    ScrollablePanel(modifier = modifier, contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
        Text(
            text = stringResource(
                if (detail is BrowseBucketDetail.Permission) R.string.tv_browse_permission_about else R.string.tv_browse_certificate_about,
            ),
            style = AppTheme.typography.titleMedium,
        )
        fingerprint?.let { FingerprintBox(value = it, label = fingerprintLabel, modifier = Modifier.fillMaxWidth()) }
        detail?.let { BucketDetailInfo(detail = it) }
    }
}

private const val SEARCH_KEY = "search"
private const val EMPTY_KEY = "empty"
private val InfoWidth = 300.dp
private val FocusRoom = 12.dp

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvBrowseAppsContentPreview() {
    ApkAnalyzerTvTheme {
        TvBrowseAppsContent(
            dimension = BrowseDimension.Permission,
            bucketKey = "android.permission.CAMERA",
            bucketLabel = "Camera",
            subAttribute = null,
            state = TvBrowseAppsState.Loaded(
                query = "",
                totalApps = 2,
                apps = listOf(
                    TvBrowseAppItem(PackageName("com.google.android.youtube.tv"), "YouTube"),
                    TvBrowseAppItem(PackageName("com.netflix.ninja"), "Netflix"),
                ),
                bucketDetail = BrowseBucketDetail.Permission(
                    protectionLevel = ProtectionLevel.Dangerous,
                    description = "Allows the app to take pictures and videos with any camera.",
                ),
            ),
            onAction = {},
        )
    }
}
