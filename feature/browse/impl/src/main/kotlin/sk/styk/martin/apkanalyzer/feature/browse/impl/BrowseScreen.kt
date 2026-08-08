package sk.styk.martin.apkanalyzer.feature.browse.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.ChipVariant
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SkeletonBox
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.navigationBarContentPadding
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.browse.impl.model.BrowseDimension

@Composable
internal fun BrowseScreen(
    onOpenDimension: (BrowseDimension) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowseEvent.NavigateToDimension -> onOpenDimension(event.dimension)
            }
        }
    }

    BrowseContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun BrowseContent(
    state: BrowseState,
    onAction: (BrowseAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        BrowseHeader(state = state)

        when (state) {
            is BrowseState.Loading -> BrowseLoadingList()

            is BrowseState.Loaded -> Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = navigationBarContentPadding + 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.dimensions.forEach { summary ->
                    DimensionCard(
                        summary = summary,
                        onClick = { onAction(BrowseAction.SelectDimension(summary.dimension)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseHeader(state: BrowseState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
        Text(
            text = stringResource(R.string.browse_title),
            style = AppTheme.typography.headlineSmall,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when (state) {
                is BrowseState.Loading -> stringResource(R.string.browse_subtitle_loading)
                is BrowseState.Loaded -> pluralStringResource(R.plurals.browse_subtitle, state.totalApps, state.totalApps)
            },
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun DimensionCard(
    summary: DimensionSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(44.dp)
                    .background(AppTheme.colors.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = summary.dimension.icon(),
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.dimension.title(),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary.dimension.subtitle(),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = ApkAnalyzerIcons.ChevronRight,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        if (summary.topLabels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState(), enabled = false)
                    .padding(start = 60.dp),
            ) {
                summary.topLabels.forEach { label ->
                    Chip(label = label, variant = ChipVariant.Tonal)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = pluralStringResource(R.plurals.browse_dimension_option_count, summary.optionCount, summary.optionCount),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.primary,
            modifier = Modifier.padding(start = 60.dp),
        )
    }
}

@Composable
private fun BrowseLoadingList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(5) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.CardShape)
                    .background(AppTheme.colors.surface)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(modifier = Modifier.size(44.dp).clip(CircleShape))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BrowseContentLoadingPreview() {
    ApkAnalyzerTheme {
        BrowseContent(state = BrowseState.Loading, onAction = {})
    }
}

@Preview
@Composable
private fun BrowseContentLoadedPreview() {
    ApkAnalyzerTheme {
        BrowseContent(state = sampleLoadedState(), onAction = {})
    }
}

private fun sampleLoadedState() = BrowseState.Loaded(
    totalApps = 187,
    dimensions = persistentListOf(
        DimensionSummary(
            dimension = BrowseDimension.Permission,
            optionCount = 143,
            topLabels = persistentListOf("Full network access", "Camera", "Precise location", "Contacts"),
        ),
        DimensionSummary(
            dimension = BrowseDimension.SigningCertificate,
            optionCount = 96,
            topLabels = persistentListOf("Google LLC", "Samsung Electronics", "Unknown signer"),
        ),
        DimensionSummary(
            dimension = BrowseDimension.TargetSdk,
            optionCount = 12,
            topLabels = persistentListOf("Android 14", "Android 13", "Android 15"),
        ),
        DimensionSummary(
            dimension = BrowseDimension.MinSdk,
            optionCount = 9,
            topLabels = persistentListOf("Android 8.0", "Android 7.0", "Android 10"),
        ),
        DimensionSummary(
            dimension = BrowseDimension.InstallSource,
            optionCount = 3,
            topLabels = persistentListOf("Google Play", "System", "Unknown"),
        ),
    ).toImmutableList(),
)
