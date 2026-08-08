package sk.styk.martin.apkanalyzer.feature.browse.impl.options

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
import androidx.compose.foundation.layout.size
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
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SelectorChip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeader
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeaderContainer
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.rememberCollapsingHeaderState
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.browse.impl.R
import sk.styk.martin.apkanalyzer.feature.browse.impl.domain.subAttributes
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension
import sk.styk.martin.apkanalyzer.feature.browse.impl.title

@Composable
internal fun BrowseOptionsScreen(
    dimension: BrowseDimension,
    onBack: () -> Unit,
    onOpenOption: (bucketKey: String, bucketLabel: String, subAttribute: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseOptionsViewModel = hiltViewModel { factory: BrowseOptionsViewModel.Factory -> factory.create(dimension) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowseOptionsEvent.NavigateToApps -> onOpenOption(event.bucketKey, event.bucketLabel, event.subAttribute)
            }
        }
    }

    BrowseOptionsContent(
        dimension = dimension,
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun BrowseOptionsContent(
    dimension: BrowseDimension,
    state: BrowseOptionsState,
    onAction: (BrowseOptionsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(title = dimension.title(), onBack = onBack)

        when (state) {
            is BrowseOptionsState.Loading -> Box(modifier = Modifier.fillMaxSize()) {
                LoadingSpinner(modifier = Modifier.align(Alignment.Center))
            }

            is BrowseOptionsState.Loaded -> BrowseOptionsList(
                dimension = dimension,
                showSearch = dimension != BrowseDimension.InstallSource,
                state = state,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun BrowseOptionsList(
    dimension: BrowseDimension,
    showSearch: Boolean,
    state: BrowseOptionsState.Loaded,
    onAction: (BrowseOptionsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsingState = rememberCollapsingHeaderState()
    val subAttributes = dimension.subAttributes()

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .collapsingHeaderContainer(collapsingState),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { collapsingState.headerOffset }
                .background(AppTheme.colors.background)
                .collapsingHeader(collapsingState),
        ) {
            if (showSearch) {
                SearchBarActive(
                    query = state.query,
                    placeholder = pluralStringResource(R.plurals.browse_options_filter_hint, state.totalOptions, state.totalOptions),
                    onQueryChange = { onAction(BrowseOptionsAction.ChangeQuery(it)) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (subAttributes.isNotEmpty()) {
                val selected = subAttributes.firstOrNull { it.key == state.subAttribute } ?: subAttributes.first()
                SelectorChip(
                    sheetTitle = stringResource(R.string.browse_sub_attribute_sheet_title),
                    options = subAttributes,
                    selected = selected,
                    label = { stringResource(it.labelRes) },
                    onSelectOption = { onAction(BrowseOptionsAction.SelectSubAttribute(it.key)) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (state.options.isNotEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { collapsingState.contentOffset },
            ) {
                items(items = state.options, key = { it.key }) { option ->
                    BrowseOptionRow(
                        option = option,
                        onClick = { onAction(BrowseOptionsAction.SelectOption(option)) },
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.browse_options_empty_query, state.query),
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
private fun BrowseOptionRow(
    option: BrowseOption,
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (option.rawIdentifier != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.rawIdentifier,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = pluralStringResource(R.plurals.browse_apps_count, option.count, option.count),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.primary,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = ApkAnalyzerIcons.ChevronRight,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview
@Composable
private fun BrowseOptionsLoadingPreview() {
    ApkAnalyzerTheme {
        BrowseOptionsContent(dimension = BrowseDimension.Permission, state = BrowseOptionsState.Loading, onAction = {}, onBack = {})
    }
}

@Preview
@Composable
private fun BrowseOptionsLoadedPreview() {
    ApkAnalyzerTheme {
        BrowseOptionsContent(dimension = BrowseDimension.Permission, state = sampleLoadedState(), onAction = {}, onBack = {})
    }
}

@Preview
@Composable
private fun BrowseOptionsCertificatePreview() {
    ApkAnalyzerTheme {
        BrowseOptionsContent(
            dimension = BrowseDimension.SigningCertificate,
            state = sampleLoadedState().copy(
                subAttribute = "fingerprint",
                options = persistentListOf(
                    BrowseOption(key = "a1b2c3", label = "A1:B2:C3:D4:E5:F6:A7:B8…", rawIdentifier = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0", count = 12),
                    BrowseOption(key = "f9e8d7", label = "F9:E8:D7:C6:B5:A4:93:82…", rawIdentifier = "F9:E8:D7:C6:B5:A4:93:82:71:60", count = 3),
                ).toImmutableList(),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun BrowseOptionsNoSearchPreview() {
    ApkAnalyzerTheme {
        BrowseOptionsContent(
            dimension = BrowseDimension.InstallSource,
            state = sampleLoadedState().copy(
                options = persistentListOf(
                    BrowseOption(key = "GooglePlay", label = "Google Play", rawIdentifier = null, count = 150),
                    BrowseOption(key = "SystemPreinstalled", label = "System", rawIdentifier = null, count = 30),
                    BrowseOption(key = "Unknown", label = "Unknown", rawIdentifier = null, count = 7),
                ).toImmutableList(),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun BrowseOptionsEmptyPreview() {
    ApkAnalyzerTheme {
        BrowseOptionsContent(
            dimension = BrowseDimension.Permission,
            state = sampleLoadedState().copy(query = "xyz", options = persistentListOf()),
            onAction = {},
            onBack = {},
        )
    }
}

private fun sampleLoadedState() = BrowseOptionsState.Loaded(
    query = "",
    subAttribute = null,
    totalOptions = 3,
    options = persistentListOf(
        BrowseOption(key = "android.permission.INTERNET", label = "Full network access", rawIdentifier = "android.permission.INTERNET", count = 176),
        BrowseOption(key = "android.permission.CAMERA", label = "Camera", rawIdentifier = "android.permission.CAMERA", count = 24),
        BrowseOption(
            key = "android.permission.ACCESS_FINE_LOCATION",
            label = "Precise location",
            rawIdentifier = "android.permission.ACCESS_FINE_LOCATION",
            count = 18,
        ),
    ).toImmutableList(),
)
