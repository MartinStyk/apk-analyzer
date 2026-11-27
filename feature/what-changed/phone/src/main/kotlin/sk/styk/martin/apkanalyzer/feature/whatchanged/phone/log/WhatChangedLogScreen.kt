package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.log

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryChangeCategory
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Chip
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ListSectionHeader
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.format.historyMonth
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.R
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeKind
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.HistoryEmpty
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.HistoryError
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.HistoryLoading
import java.time.Instant
import java.time.YearMonth

@Composable
internal fun WhatChangedLogScreen(
    onBack: () -> Unit,
    onNavigateToChange: (packageName: String, snapshotId: Long, appName: String) -> Unit,
    onNavigateToFullDetail: (AppDetailInput.Historical) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WhatChangedLogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                WhatChangedLogEvent.NavigateBack -> onBack()
                is WhatChangedLogEvent.NavigateToChange -> onNavigateToChange(event.packageName, event.snapshotId, event.appName)
                is WhatChangedLogEvent.NavigateToFullDetail -> onNavigateToFullDetail(event.input)
                WhatChangedLogEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.what_changed_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    WhatChangedLogContent(state = state, onAction = viewModel::onAction, modifier = modifier)
}

@Composable
private fun WhatChangedLogContent(
    state: WhatChangedLogState,
    onAction: (WhatChangedLogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(title = stringResource(R.string.what_changed_full_history), onBack = { onAction(WhatChangedLogAction.Back) })
        when (state) {
            WhatChangedLogState.Loading -> HistoryLoading()
            WhatChangedLogState.Error -> HistoryError(onRetry = { onAction(WhatChangedLogAction.Retry) })
            WhatChangedLogState.Empty -> HistoryEmpty()
            is WhatChangedLogState.Loaded -> LoadedContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun LoadedContent(
    state: WhatChangedLogState.Loaded,
    onAction: (WhatChangedLogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier = modifier.fillMaxSize()) {
        if (state.filters.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                state.filters.forEach { filter ->
                    Chip(
                        label = filter.label(),
                        selected = filter in state.selectedFilters,
                        onClick = { onAction(WhatChangedLogAction.ToggleFilter(filter)) },
                    )
                }
            }
        }
        if (state.months.isEmpty()) {
            Text(
                text = stringResource(R.string.what_changed_log_no_match),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            state.months.forEachIndexed { index, month ->
                stickyHeader(key = "month-${month.month}") {
                    ListSectionHeader(
                        title = month.month.historyMonth(locale).uppercase(locale),
                        topPadding = if (index == 0) 0.dp else 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.colors.background),
                    )
                }
                itemsPositioned(items = month.items, key = { _, item -> item.key }) { position, item ->
                    AppChangeRow(
                        item = item,
                        position = position,
                        onClick = { onAction(WhatChangedLogAction.OpenItem(item)) },
                        onLongClick = { copyText -> onAction(WhatChangedLogAction.CopyItem(item.appName, copyText)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LogFilter.label(): String = when (this) {
    LogFilter.NeedsAttention -> stringResource(R.string.what_changed_filter_attention)

    is LogFilter.Category -> stringResource(
        when (category) {
            AppHistoryChangeCategory.Version, AppHistoryChangeCategory.SdkLevels -> R.string.what_changed_category_android_versions
            AppHistoryChangeCategory.Size -> R.string.what_changed_category_size
            AppHistoryChangeCategory.InstallSource -> R.string.what_changed_category_install_source
            AppHistoryChangeCategory.ManifestFlags -> R.string.what_changed_category_settings
            AppHistoryChangeCategory.Permissions -> R.string.what_changed_category_permissions
            AppHistoryChangeCategory.Components -> R.string.what_changed_category_components
            AppHistoryChangeCategory.Signing -> R.string.what_changed_category_signing
            AppHistoryChangeCategory.NativeLibraries -> R.string.what_changed_category_native
            AppHistoryChangeCategory.Requirements -> R.string.what_changed_filter_requirements
        },
    )
}

private val PreviewDate = Instant.parse("2026-11-26T10:00:00Z")

private val PreviewItem = AppChangeItem(
    packageName = "com.instagram.android",
    appName = "Instagram",
    snapshotId = 4,
    capturedAt = PreviewDate,
    date = PreviewDate,
    kind = AppChangeKind.Change(AppHistoryEntry.Change.Kind.Updated),
    version = AppHistoryVersion(315, "315.0"),
    versionChange = ValueChange(AppHistoryVersion(312, "312.0"), AppHistoryVersion(315, "315.0")),
    parts = listOf(AppHistorySummaryPart.DangerousPermissionsAdded("Camera", 0), AppHistorySummaryPart.ComponentsAdded(2)),
    categories = setOf(AppHistoryChangeCategory.Permissions, AppHistoryChangeCategory.Components),
    needsAttention = true,
)

private val PreviewLoaded = WhatChangedLogState.Loaded(
    months = listOf(
        AppChangeMonth(
            YearMonth.of(2026, 11),
            listOf(
                PreviewItem,
                PreviewItem.copy(packageName = "com.whatsapp", appName = "WhatsApp", snapshotId = 7, parts = emptyList(), hasCertificateChange = true),
            ),
        ),
        AppChangeMonth(
            YearMonth.of(2026, 10),
            listOf(
                PreviewItem.copy(
                    packageName = "com.torch.pro",
                    appName = "Torch Pro",
                    snapshotId = 1,
                    date = Instant.parse("2026-10-14T10:00:00Z"),
                    kind = AppChangeKind.FirstRecorded,
                    parts = emptyList(),
                ),
            ),
        ),
    ),
    filters = listOf(
        LogFilter.NeedsAttention,
        LogFilter.Category(AppHistoryChangeCategory.Permissions),
        LogFilter.Category(AppHistoryChangeCategory.Components),
    ),
    selectedFilters = emptySet(),
)

@Preview
@Composable
private fun WhatChangedLogPreview() {
    ApkAnalyzerTheme {
        WhatChangedLogContent(state = PreviewLoaded, onAction = {})
    }
}

@Preview
@Composable
private fun WhatChangedLogNoMatchPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        WhatChangedLogContent(
            state = PreviewLoaded.copy(months = emptyList(), selectedFilters = setOf(LogFilter.NeedsAttention)),
            onAction = {},
        )
    }
}
