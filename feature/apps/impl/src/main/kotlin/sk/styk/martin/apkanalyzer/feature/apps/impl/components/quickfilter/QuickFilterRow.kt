package sk.styk.martin.apkanalyzer.feature.apps.impl.components.quickfilter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentSetOf
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.sharedElement
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.apps.impl.R
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.QuickFilter

@Composable
internal fun QuickFilterRow(
    onFilter: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickFilterRowViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuickFilterRowEvent.NavigateToFilter -> onFilter()
            }
        }
    }

    QuickFilterRowContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun QuickFilterRowContent(
    state: QuickFilterRowState,
    onAction: (QuickFilterRowAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .sharedElement("quick-filters"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip(
            label = stringResource(R.string.filter),
            selected = state.isDeepFilterActive,
            trailingIcon = ApkAnalyzerIcons.Filter,
            onClick = { onAction(QuickFilterRowAction.FilterClick) },
        )

        QuickFilter.entries.forEach { quickFilter ->
            Chip(
                label = quickFilter.displayName(),
                selected = quickFilter in state.activeQuickFilters,
                onClick = { onAction(QuickFilterRowAction.QuickFilterToggle(quickFilter)) },
            )
        }
    }
}

@Composable
private fun QuickFilter.displayName(): String = when (this) {
    QuickFilter.LargeApps -> stringResource(R.string.quick_filter_large)
    QuickFilter.SystemApps -> stringResource(R.string.quick_filter_system)
    QuickFilter.Sideloaded -> stringResource(R.string.quick_filter_sideloaded)
    QuickFilter.RecentlyInstalled -> stringResource(R.string.quick_filter_recently_installed)
    QuickFilter.RecentlyUpdated -> stringResource(R.string.quick_filter_recently_updated)
}

@Preview
@Composable
private fun QuickFilterRowPreview() {
    ApkAnalyzerTheme {
        QuickFilterRowContent(
            state = QuickFilterRowState(),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun QuickFilterRowActivePreview() {
    ApkAnalyzerTheme {
        QuickFilterRowContent(
            state = QuickFilterRowState(
                activeQuickFilters = persistentSetOf(QuickFilter.LargeApps, QuickFilter.SystemApps),
                isDeepFilterActive = true,
            ),
            onAction = {},
        )
    }
}
