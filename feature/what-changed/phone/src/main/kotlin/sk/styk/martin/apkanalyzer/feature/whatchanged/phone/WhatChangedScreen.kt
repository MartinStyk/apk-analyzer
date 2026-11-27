package sk.styk.martin.apkanalyzer.feature.whatchanged.phone

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Chip
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ListSectionHeader
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.navigationBarContentPadding
import sk.styk.martin.apkanalyzer.core.designsystem.phone.format.historyDate
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeKind
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppHistoryAppRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.HistoryEmpty
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.HistoryError
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.HistoryLoading
import java.time.Instant

@Composable
internal fun WhatChangedScreen(
    onNavigateToFullHistory: () -> Unit,
    onNavigateToChange: (packageName: String, snapshotId: Long, appName: String) -> Unit,
    onNavigateToFullDetail: (AppDetailInput.Historical) -> Unit,
    onNavigateToAppHistory: (packageName: String, appName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WhatChangedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                WhatChangedEvent.NavigateToFullHistory -> onNavigateToFullHistory()
                is WhatChangedEvent.NavigateToChange -> onNavigateToChange(event.packageName, event.snapshotId, event.appName)
                is WhatChangedEvent.NavigateToFullDetail -> onNavigateToFullDetail(event.input)
                is WhatChangedEvent.NavigateToAppHistory -> onNavigateToAppHistory(event.packageName, event.appName)
                WhatChangedEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.what_changed_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    WhatChangedContent(state = state, onAction = viewModel::onAction, modifier = modifier)
}

@Composable
private fun WhatChangedContent(
    state: WhatChangedState,
    onAction: (WhatChangedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Header(onOpenFullHistory = { onAction(WhatChangedAction.OpenFullHistory) })
        when (state) {
            WhatChangedState.Loading -> HistoryLoading()

            WhatChangedState.Error -> HistoryError(onRetry = { onAction(WhatChangedAction.Retry) })

            WhatChangedState.Empty -> HistoryEmpty()

            is WhatChangedState.Loaded -> {
                RangeChips(selected = state.range, onSelectRange = { onAction(WhatChangedAction.SelectRange(it)) })
                LoadedContent(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun Header(onOpenFullHistory: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.what_changed_title),
            style = AppTheme.typography.headlineSmall,
            color = AppTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            text = stringResource(R.string.what_changed_full_history),
            onClick = onOpenFullHistory,
        )
    }
}

@Composable
private fun LoadedContent(
    state: WhatChangedState.Loaded,
    onAction: (WhatChangedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navigationBarContentPadding + 16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "summary") {
            Summary(state = state)
        }
        changeSection(
            key = "attention",
            title = R.string.what_changed_section_attention,
            items = state.needsAttention,
            onAction = onAction,
            uppercase = { it.uppercase(locale) },
        )
        changedAppsSection(state = state, onAction = onAction, uppercase = { it.uppercase(locale) })
        if (state.uninstalledApps.isNotEmpty()) {
            item(key = "uninstalled-header") { ListSectionHeader(title = stringResource(R.string.what_changed_section_uninstalled).uppercase(locale)) }
            itemsPositioned(items = state.uninstalledApps, key = { _, app -> "uninstalled-${app.packageName}" }) { position, app ->
                AppHistoryAppRow(
                    packageName = app.packageName,
                    appName = app.appName,
                    supportingText = if (app.changeCount == 0) {
                        stringResource(R.string.what_changed_no_changes_recorded)
                    } else {
                        pluralStringResource(R.plurals.what_changed_change_count, app.changeCount, app.changeCount)
                    },
                    position = position,
                    onClick = { onAction(WhatChangedAction.OpenAppHistory(app.packageName, app.appName)) },
                )
            }
        }
    }
}

private fun LazyListScope.changeSection(
    key: String,
    @StringRes title: Int,
    items: List<AppChangeItem>,
    onAction: (WhatChangedAction) -> Unit,
    uppercase: (String) -> String,
) {
    if (items.isEmpty()) return
    item(key = "$key-header") { ListSectionHeader(title = uppercase(stringResource(title))) }
    itemsPositioned(items = items, key = { _, item -> "$key-${item.key}" }) { position, item ->
        AppChangeRow(
            item = item,
            position = position,
            onClick = { onAction(WhatChangedAction.OpenChange(item)) },
            onLongClick = { copyText -> onAction(WhatChangedAction.CopyItem(item.appName, copyText)) },
        )
    }
}

private fun LazyListScope.changedAppsSection(
    state: WhatChangedState.Loaded,
    onAction: (WhatChangedAction) -> Unit,
    uppercase: (String) -> String,
) {
    if (state.changedApps.isEmpty()) {
        val lastChangeAt = state.lastChangeAt ?: return
        item(key = "changed-header") { ListSectionHeader(title = uppercase(stringResource(R.string.what_changed_section_changed_apps))) }
        item(key = "changed-empty") {
            Text(
                text = stringResource(R.string.what_changed_last_change_hint, lastChangeAt.historyDate()),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        return
    }
    item(key = "changed-header") { ListSectionHeader(title = uppercase(stringResource(R.string.what_changed_section_changed_apps))) }
    itemsPositioned(items = state.changedApps, key = { _, app -> "changed-${app.packageName}" }) { position, app ->
        val netChange = app.netChange as? NetChange.Changed
        AppHistoryAppRow(
            packageName = app.packageName,
            appName = app.appName,
            supportingText = app.supportingText(),
            parts = netChange?.parts.orEmpty(),
            isWarning = netChange?.hasCertificateChange == true,
            position = position,
            onClick = { onAction(WhatChangedAction.OpenAppHistory(app.packageName, app.appName)) },
        )
    }
}

@Composable
private fun ChangedAppItem.supportingText(): String {
    val resources = LocalResources.current
    val parts = buildList {
        if (updateCount > 0) add(resources.getQuantityString(R.plurals.what_changed_updates, updateCount, updateCount))
        if (isNew) add(resources.getString(R.string.what_changed_new_app))
        when (val change = netChange) {
            NetChange.None -> add(resources.getString(R.string.what_changed_no_net_change))
            is NetChange.Changed -> if (change.hasCertificateChange) add(resources.getString(R.string.what_changed_certificate_changed))
            NetChange.NotCompared -> Unit
        }
    }
    return parts.joinToString(separator = stringResource(R.string.what_changed_separator))
}

@Composable
private fun RangeChips(
    selected: WhatChangedRange,
    onSelectRange: (WhatChangedRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = WhatChangedRange.entries.indexOf(selected))
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(items = WhatChangedRange.entries, key = { it.name }) { range ->
            Chip(label = stringResource(range.labelRes), selected = range == selected, onClick = { onSelectRange(range) })
        }
    }
}

@Composable
private fun Summary(state: WhatChangedState.Loaded, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    Column(modifier = modifier.padding(top = 4.dp, bottom = 4.dp)) {
        val counts = buildList {
            if (state.updateCount > 0) add(resources.getQuantityString(R.plurals.what_changed_updates, state.updateCount, state.updateCount))
            if (state.newAppCount > 0) add(resources.getQuantityString(R.plurals.what_changed_new_apps, state.newAppCount, state.newAppCount))
            if (state.needsAttention.isNotEmpty()) {
                add(resources.getQuantityString(R.plurals.what_changed_attention_count, state.needsAttention.size, state.needsAttention.size))
            }
        }
        val since = state.since.historyDate()
        Text(
            text = stringResource(if (counts.isEmpty()) R.string.what_changed_nothing_since else R.string.what_changed_since, since),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (counts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = counts.joinToString(separator = stringResource(R.string.what_changed_separator)),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

private val PreviewNow = Instant.parse("2026-11-26T10:00:00Z")

private val PreviewChange = AppChangeItem(
    packageName = "com.instagram.android",
    appName = "Instagram",
    snapshotId = 4,
    capturedAt = PreviewNow,
    date = PreviewNow,
    kind = AppChangeKind.Change(AppHistoryEntry.Change.Kind.Updated),
    version = AppHistoryVersion(315, "315.0"),
    versionChange = ValueChange(AppHistoryVersion(312, "312.0"), AppHistoryVersion(315, "315.0")),
    parts = listOf(AppHistorySummaryPart.DangerousPermissionsAdded("Camera", 0), AppHistorySummaryPart.SizeChanged(120.megabytes, 128.megabytes)),
    needsAttention = true,
)

private val PreviewLoaded = WhatChangedState.Loaded(
    range = WhatChangedRange.SinceLastVisit,
    since = PreviewNow.minusSeconds(7 * 86_400L),
    updateCount = 6,
    newAppCount = 2,
    lastChangeAt = null,
    needsAttention = listOf(
        PreviewChange,
        PreviewChange.copy(packageName = "com.whatsapp", appName = "WhatsApp", snapshotId = 9, parts = emptyList(), hasCertificateChange = true),
    ),
    changedApps = listOf(
        ChangedAppItem("com.instagram.android", "Instagram", 2, false, NetChange.Changed(PreviewChange.parts, hasCertificateChange = false)),
        ChangedAppItem("org.thoughtcrime.securesms", "Signal", 0, true, NetChange.NotCompared),
        ChangedAppItem("com.android.chrome", "Chrome", 2, false, NetChange.None),
    ),
    uninstalledApps = listOf(UninstalledAppItem("com.torch.pro", "Torch Pro", 5)),
)

@Preview(heightDp = 1300)
@Composable
private fun WhatChangedLoadedPreview() {
    ApkAnalyzerTheme {
        WhatChangedContent(state = PreviewLoaded, onAction = {})
    }
}

@Preview
@Composable
private fun WhatChangedNothingInRangePreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        WhatChangedContent(
            state = PreviewLoaded.copy(
                updateCount = 0,
                newAppCount = 0,
                lastChangeAt = PreviewNow.minusSeconds(16 * 86_400L),
                needsAttention = emptyList(),
                changedApps = emptyList(),
                uninstalledApps = emptyList(),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun WhatChangedEmptyPreview() {
    ApkAnalyzerTheme {
        WhatChangedContent(state = WhatChangedState.Empty, onAction = {})
    }
}
