package sk.styk.martin.apkanalyzer.feature.browse.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseOption
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.core.appindex.browse.titleRes
import sk.styk.martin.apkanalyzer.core.common.digest.toFingerprint
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.LocalFingerprintFormat
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.InfoCard
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.SectionHeader
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.RestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.rememberRestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.restorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.browse.tv.components.BrowseLoading
import sk.styk.martin.apkanalyzer.core.appindex.R as AppIndexR
import sk.styk.martin.apkanalyzer.feature.browse.api.R as BrowseApiR

@Composable
internal fun TvBrowseScreen(
    onOpenDimension: (BrowseDimension) -> Unit,
    onOpenOption: (dimension: BrowseDimension, bucketKey: String, bucketLabel: String, subAttribute: BrowseSubAttribute?) -> Unit,
    viewModel: TvBrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TvBrowseEvent.NavigateToDimension -> onOpenDimension(event.dimension)
                is TvBrowseEvent.NavigateToApps -> onOpenOption(event.dimension, event.bucketKey, event.bucketLabel, event.subAttribute)
            }
        }
    }

    TvBrowseContent(state = state, onAction = viewModel::onAction)
}

@Composable
private fun TvBrowseContent(state: TvBrowseState, onAction: (TvBrowseAction) -> Unit) {
    when (state) {
        TvBrowseState.Loading -> BrowseLoading()
        is TvBrowseState.Loaded -> LoadedContent(state = state, onAction = onAction)
    }
}

@Composable
private fun LoadedContent(state: TvBrowseState.Loaded, onAction: (TvBrowseAction) -> Unit) {
    val focus = rememberRestorableFocus(
        availableKeys = buildSet {
            state.rows.forEach { row ->
                row.options.forEach { add(optionKey(row.dimension, it.option)) }
                add(seeAllKey(row.dimension))
            }
        },
        defaultKey = state.rows.firstOrNull()?.let { row ->
            row.options.firstOrNull()?.let { optionKey(row.dimension, it.option) } ?: seeAllKey(row.dimension)
        }.orEmpty(),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 27.dp, bottom = 27.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = HEADER_KEY) {
            Column(modifier = Modifier.padding(horizontal = HorizontalPadding)) {
                Text(text = stringResource(BrowseApiR.string.browse), style = AppTheme.typography.headlineSmall)
                Text(
                    text = pluralStringResource(AppIndexR.plurals.browse_subtitle, state.totalApps, state.totalApps),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
        }
        items(state.rows, key = { it.dimension.name }) { row ->
            DimensionRow(row = row, focus = focus, onAction = onAction)
        }
    }
}

@Composable
private fun DimensionRow(
    row: TvBrowseRow,
    focus: RestorableFocus,
    onAction: (TvBrowseAction) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.padding(start = HorizontalPadding, end = HorizontalPadding, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(text = stringResource(row.dimension.titleRes))
            Text(
                text = pluralStringResource(AppIndexR.plurals.browse_dimension_option_count, row.optionCount, row.optionCount),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = HorizontalPadding, vertical = FocusRoom),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(row.options, key = { it.option.key }) { rowOption ->
                OptionTile(
                    rowOption = rowOption,
                    icon = row.dimension.bucketIcon(rowOption.option.key),
                    onClick = { onAction(TvBrowseAction.SelectOption(row.dimension, rowOption.option, row.subAttribute)) },
                    modifier = Modifier.restorableFocus(focus, optionKey(row.dimension, rowOption.option)),
                )
            }
            item(key = SEE_ALL_KEY) {
                InfoCard(
                    onClick = { onAction(TvBrowseAction.SelectDimension(row.dimension)) },
                    title = stringResource(R.string.tv_browse_see_all),
                    icon = ApkAnalyzerIcons.ChevronRight,
                    stackedHeader = true,
                    modifier = Modifier
                        .restorableFocus(focus, seeAllKey(row.dimension))
                        .size(width = SeeAllWidth, height = TileHeight),
                ) {
                    Text(
                        text = pluralStringResource(AppIndexR.plurals.browse_dimension_option_count, row.optionCount, row.optionCount),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionTile(
    rowOption: TvBrowseRowOption,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoCard(
        onClick = onClick,
        title = when (val option = rowOption.option) {
            is BrowseOption.CertificateHash -> option.key.toFingerprint(LocalFingerprintFormat.current)
            is BrowseOption.Labeled -> option.label
        },
        icon = icon,
        stackedHeader = true,
        modifier = modifier.size(width = TileWidth, height = TileHeight),
    ) {
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowOption.packages.forEach { packageName ->
                    AppIcon(source = AppReference.InstalledPackage(packageName), size = 26.dp)
                }
            }
            Text(
                text = pluralStringResource(AppIndexR.plurals.browse_apps_count, rowOption.option.count, rowOption.option.count),
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.primary,
            )
        }
    }
}

private fun optionKey(dimension: BrowseDimension, option: BrowseOption) = "${dimension.name}:option:${option.key}"

private fun seeAllKey(dimension: BrowseDimension) = "${dimension.name}:all"

private const val HEADER_KEY = "header"
private const val SEE_ALL_KEY = "see-all"
private val HorizontalPadding = 48.dp
private val FocusRoom = 12.dp
private val TileWidth = 240.dp
private val SeeAllWidth = 160.dp
private val TileHeight = 116.dp

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvBrowseContentPreview() {
    val packages = listOf(PackageName("com.google.android.youtube.tv"), PackageName("com.netflix.ninja"), PackageName("com.spotify.tv.android"))
    ApkAnalyzerTvTheme {
        TvBrowseContent(
            state = TvBrowseState.Loaded(
                totalApps = 142,
                rows = listOf(
                    TvBrowseRow(
                        dimension = BrowseDimension.Permission,
                        subAttribute = null,
                        optionCount = 186,
                        options = listOf(
                            TvBrowseRowOption(
                                BrowseOption.Labeled("android.permission.INTERNET", "Full network access", "android.permission.INTERNET", 176),
                                packages,
                            ),
                            TvBrowseRowOption(BrowseOption.Labeled("android.permission.CAMERA", "Camera", "android.permission.CAMERA", 24), packages),
                        ),
                    ),
                    TvBrowseRow(
                        dimension = BrowseDimension.SigningCertificate,
                        subAttribute = BrowseSubAttribute.CertificateOrganization,
                        optionCount = 24,
                        options = listOf(TvBrowseRowOption(BrowseOption.Labeled("Google LLC", "Google LLC", null, 38), packages)),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
