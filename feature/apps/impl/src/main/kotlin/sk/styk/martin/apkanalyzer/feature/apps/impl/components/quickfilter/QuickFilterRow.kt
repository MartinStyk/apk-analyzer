package sk.styk.martin.apkanalyzer.feature.apps.impl.components.quickfilter

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.MultiSelectorChip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SelectorChip
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.sharedElement
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.apps.impl.R
import sk.styk.martin.apkanalyzer.feature.apps.impl.components.AppDataPermission
import sk.styk.martin.apkanalyzer.feature.apps.impl.components.PermissionRationaleBottomSheet
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.ActivityQuickFilter
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.QuickFilter
import sk.styk.martin.apkanalyzer.feature.apps.impl.filter.domain.SourceQuickFilter

@Composable
internal fun QuickFilterRow(
    onFilter: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickFilterRowViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuickFilterRowEvent.NavigateToFilter -> onFilter()

                is QuickFilterRowEvent.OpenUsageAccessSettings -> {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
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

        MultiSelectorChip(
            sheetTitle = stringResource(R.string.quick_filter_source_sheet_title),
            defaultLabel = stringResource(R.string.quick_filter_source_sheet_title),
            options = persistentListOf(SourceQuickFilter.System, SourceQuickFilter.GooglePlay, SourceQuickFilter.Sideloaded),
            selected = state.activeSourceQuickFilters,
            optionLabel = { it.displayName() },
            selectionLabel = { it.selectionLabel() },
            onToggleOption = { onAction(QuickFilterRowAction.SourceQuickFilterToggled(it)) },
        )

        SelectorChip(
            sheetTitle = stringResource(R.string.quick_filter_activity_sheet_title),
            options = persistentListOf(null, ActivityQuickFilter.RecentlyUsed, ActivityQuickFilter.Unused),
            selected = state.activeActivityQuickFilter,
            label = { it.displayName() },
            chipLabel = { it.chipDisplayName() },
            onSelectOption = { onAction(QuickFilterRowAction.ActivityQuickFilterSelected(it)) },
            isActive = state.activeActivityQuickFilter != null,
        )

        QuickFilter.entries.forEach { quickFilter ->
            Chip(
                label = quickFilter.displayName(),
                selected = quickFilter in state.activeQuickFilters,
                onClick = { onAction(QuickFilterRowAction.QuickFilterToggle(quickFilter)) },
            )
        }
    }

    state.permissionRationale?.let { rationale ->
        PermissionRationaleBottomSheet(
            title = stringResource(rationale.titleRes()),
            description = stringResource(rationale.descriptionRes()),
            openSettingsLabel = stringResource(R.string.quick_filter_permission_open_settings),
            onOpenSettings = { onAction(QuickFilterRowAction.OpenPermissionSettings(rationale)) },
            onDismiss = { onAction(QuickFilterRowAction.DismissPermissionRationale) },
        )
    }
}

@Composable
private fun QuickFilter.displayName(): String = when (this) {
    QuickFilter.SensitivePermissions -> stringResource(R.string.quick_filter_sensitive_permissions)
    QuickFilter.Large -> stringResource(R.string.quick_filter_large_total)
    QuickFilter.RecentlyInstalled -> stringResource(R.string.quick_filter_recently_installed)
    QuickFilter.RecentlyUpdated -> stringResource(R.string.quick_filter_recently_updated)
}

@Composable
private fun SourceQuickFilter.displayName(): String = when (this) {
    SourceQuickFilter.System -> stringResource(R.string.quick_filter_system)
    SourceQuickFilter.GooglePlay -> stringResource(R.string.quick_filter_google_play)
    SourceQuickFilter.Sideloaded -> stringResource(R.string.quick_filter_sideloaded)
}

@Composable
private fun ImmutableList<SourceQuickFilter>.selectionLabel(): String {
    val firstLabel = first().displayName()
    return if (size == 1) firstLabel else stringResource(R.string.quick_filter_selection_more, firstLabel, size - 1)
}

@Composable
private fun ActivityQuickFilter?.displayName(): String = when (this) {
    null -> stringResource(R.string.quick_filter_activity_all)
    ActivityQuickFilter.RecentlyUsed -> stringResource(R.string.quick_filter_recently_used)
    ActivityQuickFilter.Unused -> stringResource(R.string.quick_filter_unused)
}

@Composable
private fun ActivityQuickFilter?.chipDisplayName(): String = when (this) {
    null -> stringResource(R.string.quick_filter_activity_sheet_title)
    else -> displayName()
}

private fun AppDataPermission.titleRes(): Int = when (this) {
    AppDataPermission.UsageAccess -> R.string.quick_filter_permission_usage_title
    AppDataPermission.StorageAccess -> R.string.quick_filter_permission_storage_title
}

private fun AppDataPermission.descriptionRes(): Int = when (this) {
    AppDataPermission.UsageAccess -> R.string.quick_filter_permission_usage_description
    AppDataPermission.StorageAccess -> R.string.quick_filter_permission_storage_description
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
                activeQuickFilters = persistentSetOf(QuickFilter.Large),
                activeSourceQuickFilters = persistentSetOf(SourceQuickFilter.GooglePlay, SourceQuickFilter.Sideloaded),
                activeActivityQuickFilter = ActivityQuickFilter.RecentlyUsed,
                isDeepFilterActive = true,
            ),
            onAction = {},
        )
    }
}
