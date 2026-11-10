package sk.styk.martin.apkanalyzer.feature.browse.tv.options

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseBucketDetail
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseDimension
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseOption
import sk.styk.martin.apkanalyzer.core.appindex.browse.BrowseSubAttribute
import sk.styk.martin.apkanalyzer.core.appindex.browse.cardLabelRes
import sk.styk.martin.apkanalyzer.core.appindex.browse.isSearchable
import sk.styk.martin.apkanalyzer.core.appindex.browse.titleRes
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.FilterChip
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.FingerprintBox
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.IconBadge
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.InfoListItem
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.ScrollablePanel
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.RestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.rememberRestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.restorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.browse.tv.R
import sk.styk.martin.apkanalyzer.feature.browse.tv.apps.TvBrowseAppItem
import sk.styk.martin.apkanalyzer.feature.browse.tv.bucketIcon
import sk.styk.martin.apkanalyzer.feature.browse.tv.components.BrowseHeader
import sk.styk.martin.apkanalyzer.feature.browse.tv.components.BrowseLoading
import sk.styk.martin.apkanalyzer.feature.browse.tv.components.BrowseSearch
import sk.styk.martin.apkanalyzer.feature.browse.tv.components.BucketDetailInfo
import sk.styk.martin.apkanalyzer.feature.browse.tv.icon
import sk.styk.martin.apkanalyzer.core.appindex.R as AppIndexR

@Composable
internal fun TvBrowseOptionsScreen(
    dimension: BrowseDimension,
    onOpenOption: (bucketKey: String, bucketLabel: String, subAttribute: BrowseSubAttribute?) -> Unit,
    viewModel: TvBrowseOptionsViewModel = hiltViewModel { factory: TvBrowseOptionsViewModel.Factory -> factory.create(dimension) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TvBrowseOptionsEvent.NavigateToApps -> onOpenOption(event.bucketKey, event.bucketLabel, event.subAttribute)
            }
        }
    }

    TvBrowseOptionsContent(dimension = dimension, state = state, onAction = viewModel::onAction)
}

@Composable
private fun TvBrowseOptionsContent(
    dimension: BrowseDimension,
    state: TvBrowseOptionsState,
    onAction: (TvBrowseOptionsAction) -> Unit,
) {
    when (state) {
        TvBrowseOptionsState.Loading -> BrowseLoading()
        is TvBrowseOptionsState.Loaded -> LoadedContent(dimension = dimension, state = state, onAction = onAction)
    }
}

@Composable
private fun LoadedContent(
    dimension: BrowseDimension,
    state: TvBrowseOptionsState.Loaded,
    onAction: (TvBrowseOptionsAction) -> Unit,
) {
    val listFocusRequester = remember { FocusRequester() }
    val focus = rememberRestorableFocus(
        availableKeys = buildSet {
            if (dimension.isSearchable) add(SEARCH_KEY)
            state.subAttributes.forEach { add(subAttributeKey(it)) }
            state.options.forEach { add(optionKey(it)) }
        },
        defaultKey = state.options.firstOrNull()?.let(::optionKey) ?: if (dimension.isSearchable) SEARCH_KEY else "",
    )

    Column(modifier = Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, top = 27.dp)) {
        BrowseHeader(
            icon = dimension.icon,
            title = stringResource(dimension.titleRes),
            subtitle = pluralStringResource(AppIndexR.plurals.browse_dimension_option_count, state.totalOptions, state.totalOptions),
        ) {
            if (dimension.isSearchable) {
                BrowseSearch(
                    query = state.query,
                    onQueryChange = { onAction(TvBrowseOptionsAction.ChangeQuery(it)) },
                    placeholder = pluralStringResource(AppIndexR.plurals.browse_options_filter_hint, state.totalOptions, state.totalOptions),
                    modifier = Modifier
                        .restorableFocus(focus, SEARCH_KEY)
                        .focusProperties { down = listFocusRequester },
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 16.dp, bottom = 27.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(modifier = Modifier.weight(LIST_WEIGHT)) {
                if (state.subAttributes.isNotEmpty()) {
                    SubAttributeChips(state = state, focus = focus, onAction = onAction)
                }
                OptionList(
                    dimension = dimension,
                    state = state,
                    focus = focus,
                    onAction = onAction,
                    modifier = Modifier.focusRequester(listFocusRequester),
                )
            }
            Crossfade(
                targetState = state.preview,
                animationSpec = tween(durationMillis = CROSSFADE_DURATION),
                modifier = Modifier.weight(PREVIEW_WEIGHT).fillMaxHeight(),
                label = "optionPreview",
            ) { preview ->
                if (preview != null) {
                    OptionPreview(
                        preview = preview,
                        icon = dimension.bucketIcon(preview.option.key),
                        onClick = { onAction(TvBrowseOptionsAction.SelectOption(preview.option)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubAttributeChips(
    state: TvBrowseOptionsState.Loaded,
    focus: RestorableFocus,
    onAction: (TvBrowseOptionsAction) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(state.subAttributes, key = ::subAttributeKey) { subAttribute ->
            FilterChip(
                selected = subAttribute == state.subAttribute,
                onClick = { onAction(TvBrowseOptionsAction.SelectSubAttribute(subAttribute)) },
                label = stringResource(subAttribute.labelRes),
                modifier = Modifier.restorableFocus(focus, subAttributeKey(subAttribute)),
            )
        }
    }
}

@Composable
private fun OptionList(
    dimension: BrowseDimension,
    state: TvBrowseOptionsState.Loaded,
    focus: RestorableFocus,
    onAction: (TvBrowseOptionsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.focusRestorer(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (state.options.isEmpty()) {
            item(key = EMPTY_KEY) {
                Text(
                    text = stringResource(AppIndexR.string.browse_options_empty_query, state.query),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        items(state.options, key = ::optionKey) { option ->
            InfoListItem(
                label = option.title(),
                value = option.summary(),
                onClick = { onAction(TvBrowseOptionsAction.SelectOption(option)) },
                leadingIcon = dimension.bucketIcon(option.key),
                trailingIcon = ApkAnalyzerIcons.ChevronRight,
                modifier = Modifier
                    .restorableFocus(focus, optionKey(option))
                    .onFocusChanged { if (it.isFocused) onAction(TvBrowseOptionsAction.FocusOption(option)) },
            )
        }
    }
}

@Composable
private fun OptionPreview(
    preview: TvBrowseOptionPreview,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScrollablePanel(modifier = modifier, resetKey = preview.option.key, spacing = 20.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(icon = icon, size = 40.dp)
            Column {
                Text(
                    text = preview.option.title(),
                    style = AppTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(AppIndexR.plurals.browse_apps_count, preview.totalApps, preview.totalApps),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.primary,
                )
            }
        }
        when (val option = preview.option) {
            is BrowseOption.CertificateHash -> FingerprintBox(
                value = option.key,
                label = stringResource(option.algorithm.cardLabelRes),
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )

            is BrowseOption.Labeled -> option.rawIdentifier?.takeIf { it != option.label }?.let {
                Text(text = it, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
            }
        }
        AppWall(apps = preview.apps, hiddenCount = preview.totalApps - preview.apps.size)
        preview.detail?.let { BucketDetailInfo(detail = it) }
    }
}

@Composable
private fun AppWall(apps: List<TvBrowseAppItem>, hiddenCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            apps.forEach { app -> AppTile(app = app) }
        }
        if (hiddenCount > 0) {
            Text(
                text = pluralStringResource(R.plurals.tv_browse_more_apps, hiddenCount, hiddenCount),
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppTile(app: TvBrowseAppItem) {
    val reference = remember(app.packageName) { AppReference.InstalledPackage(app.packageName) }
    Column(modifier = Modifier.width(AppTileWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        AppIcon(source = reference, size = 44.dp)
        Text(
            text = app.applicationName,
            style = AppTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun BrowseOption.title(): String = when (this) {
    is BrowseOption.CertificateHash -> publisher ?: stringResource(AppIndexR.string.browse_signer_unknown)
    is BrowseOption.Labeled -> label
}

@Composable
private fun BrowseOption.summary(): String {
    val apps = pluralStringResource(AppIndexR.plurals.browse_apps_count, count, count)
    return when (this) {
        is BrowseOption.CertificateHash -> stringResource(R.string.tv_browse_label_list, apps, label.take(FINGERPRINT_PREFIX_LENGTH))
        is BrowseOption.Labeled -> apps
    }
}

private fun optionKey(option: BrowseOption) = "option:${option.key}"

private fun subAttributeKey(subAttribute: BrowseSubAttribute) = "attribute:${subAttribute.name}"

private const val SEARCH_KEY = "search"
private const val EMPTY_KEY = "empty"
private const val LIST_WEIGHT = 0.42f
private const val PREVIEW_WEIGHT = 0.58f
private const val CROSSFADE_DURATION = 180
private const val FINGERPRINT_PREFIX_LENGTH = 11
private val AppTileWidth = 76.dp

private val previewApps = listOf(
    TvBrowseAppItem(PackageName("com.google.android.youtube.tv"), "YouTube"),
    TvBrowseAppItem(PackageName("com.netflix.ninja"), "Netflix"),
    TvBrowseAppItem(PackageName("com.spotify.tv.android"), "Spotify"),
)

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvBrowseOptionsContentPreview() {
    val camera = BrowseOption.Labeled("android.permission.CAMERA", "Camera", "android.permission.CAMERA", 24)
    ApkAnalyzerTvTheme {
        TvBrowseOptionsContent(
            dimension = BrowseDimension.Permission,
            state = TvBrowseOptionsState.Loaded(
                query = "",
                subAttribute = null,
                subAttributes = listOf(),
                totalOptions = 3,
                options = listOf(
                    BrowseOption.Labeled("android.permission.INTERNET", "Full network access", "android.permission.INTERNET", 176),
                    camera,
                    BrowseOption.Labeled("android.permission.ACCESS_FINE_LOCATION", "Precise location", "android.permission.ACCESS_FINE_LOCATION", 18),
                ),
                preview = TvBrowseOptionPreview(
                    option = camera,
                    detail = BrowseBucketDetail.Permission(
                        protectionLevel = ProtectionLevel.Dangerous,
                        description = "Allows the app to take pictures and videos with any camera.",
                    ),
                    apps = previewApps,
                    totalApps = 24,
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvBrowseOptionsCertificatePreview() {
    val certificate = BrowseOption.CertificateHash(
        key = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2",
        label = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2",
        algorithm = BrowseSubAttribute.CertificateSha256,
        count = 12,
        publisher = "Google LLC",
    )
    ApkAnalyzerTvTheme {
        TvBrowseOptionsContent(
            dimension = BrowseDimension.SigningCertificate,
            state = TvBrowseOptionsState.Loaded(
                query = "",
                subAttribute = BrowseSubAttribute.CertificateSha256,
                subAttributes = BrowseSubAttribute.entries,
                totalOptions = 1,
                options = listOf(certificate),
                preview = TvBrowseOptionPreview(option = certificate, detail = null, apps = previewApps, totalApps = 12),
            ),
            onAction = {},
        )
    }
}
