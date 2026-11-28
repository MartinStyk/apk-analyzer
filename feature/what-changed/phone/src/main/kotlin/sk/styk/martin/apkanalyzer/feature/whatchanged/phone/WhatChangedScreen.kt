package sk.styk.martin.apkanalyzer.feature.whatchanged.phone

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.filterNotNull
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Chip
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ListSectionHeader
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.SkeletonBox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.navigationBarContentPadding
import sk.styk.martin.apkanalyzer.core.designsystem.phone.format.historyDate
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.shape
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.ShimmerGroup
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.shimmer
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.pro.api.WhatChangedPlacement
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.access.LockableRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeItem
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeKind
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppChangeRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.AppHistoryAppRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.HistoryEmpty
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.HistoryError
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.LockedRow
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.ProUnlockBar
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components.ProUnlockCard
import java.time.Instant

private const val PRO_CARD_KEY = "pro-card"
private val ProBarSpace = 96.dp

@Composable
internal fun WhatChangedScreen(
    onNavigateToFullHistory: () -> Unit,
    onNavigateToChange: (packageName: String, snapshotId: Long, appName: String) -> Unit,
    onNavigateToFullDetail: (AppDetailInput.Historical) -> Unit,
    onNavigateToAppHistory: (packageName: String, appName: String) -> Unit,
    onNavigateToPaywall: (WhatChangedPlacement) -> Unit,
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
                is WhatChangedEvent.NavigateToPaywall -> onNavigateToPaywall(event.placement)
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
            WhatChangedState.Loading -> LoadingContent()

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
private fun LoadingContent(modifier: Modifier = Modifier) {
    ShimmerGroup {
        Column(modifier = modifier.fillMaxSize().clearAndSetSemantics {}) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(items = WhatChangedRange.entries, key = { it.name }) { range ->
                    Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(range.labelRes),
                            style = AppTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(Shapes.CardShape)
                                .shimmer(surfaceColor = AppTheme.colors.surfaceVariant, highlightColor = AppTheme.colors.surface)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navigationBarContentPadding + 16.dp),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "summary") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        LoadingText(style = AppTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(0.4f))
                        LoadingText(style = AppTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(0.85f))
                    }
                }
                item(key = "section-header") {
                    LoadingText(
                        style = AppTheme.typography.labelMedium,
                        modifier = Modifier
                            .padding(top = 20.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)
                            .fillMaxWidth(0.4f),
                    )
                }
                itemsPositioned(items = listOf(0, 1, 2, 3, 4), key = { _, index -> "loading-$index" }) { position, _ ->
                    LoadingAppRow(position = position)
                }
            }
        }
    }
}

@Composable
private fun LoadingAppRow(position: ListItemPosition, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, position.shape())
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(Shapes.CardShape)
                .shimmer(surfaceColor = AppTheme.colors.surfaceVariant, highlightColor = AppTheme.colors.surface),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LoadingText(style = AppTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(end = 32.dp))
                LoadingText(style = AppTheme.typography.labelMedium, modifier = Modifier.width(40.dp))
            }
            LoadingText(style = AppTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(0.55f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LoadingText(style = AppTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                LoadingText(style = AppTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.size(20.dp).align(Alignment.CenterVertically), contentAlignment = Alignment.Center) {
            SkeletonBox(modifier = Modifier.size(width = 8.dp, height = 12.dp))
        }
    }
}

@Composable
private fun LoadingText(style: TextStyle, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = " ",
            style = style,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
        )
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(with(density) { style.fontSize.toDp() }))
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
    val listState = rememberLazyListState()
    var cardIndex by remember { mutableIntStateOf(-1) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == PRO_CARD_KEY }?.index }
            .filterNotNull()
            .collect { cardIndex = it }
    }
    val showBar by remember(state.proCardSection) {
        derivedStateOf {
            state.proCardSection != null &&
                cardIndex >= 0 &&
                listState.firstVisibleItemIndex > cardIndex &&
                listState.layoutInfo.visibleItemsInfo.none { it.key == PRO_CARD_KEY }
        }
    }
    val bottomSpace = if (state.proCardSection == null) 16.dp else ProBarSpace
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navigationBarContentPadding + bottomSpace),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "summary") {
                Summary(state = state)
            }
            changeSection(
                key = "attention",
                title = R.string.what_changed_section_attention,
                items = state.needsAttention,
                proCardCount = state.lockedAppCount.takeIf { state.proCardSection == WhatChangedSection.NeedsAttention },
                onAction = onAction,
                uppercase = { it.uppercase(locale) },
            )
            changedAppsSection(state = state, onAction = onAction, uppercase = { it.uppercase(locale) })
            if (state.uninstalledApps.isNotEmpty()) {
                item(key = "uninstalled-header") {
                    ListSectionHeader(
                        title = stringResource(R.string.what_changed_section_uninstalled).uppercase(locale),
                        modifier = Modifier.animateItem(),
                    )
                }
                lockableRows(
                    prefix = "uninstalled",
                    rows = state.uninstalledApps,
                    proCardCount = state.lockedAppCount.takeIf { state.proCardSection == WhatChangedSection.Uninstalled },
                    onAction = onAction,
                ) { item, position ->
                    UninstalledAppRow(app = item, position = position, onAction = onAction, modifier = Modifier.animateItem())
                }
            }
        }
        AnimatedVisibility(
            visible = showBar,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = navigationBarContentPadding + 12.dp),
        ) {
            ProUnlockBar(
                lockedAppCount = state.lockedAppCount,
                onUnlock = { onAction(WhatChangedAction.OpenLocked(WhatChangedPlacement.Bar)) },
            )
        }
    }
}

private fun <T> LazyListScope.lockableRows(
    prefix: String,
    rows: List<LockableRow<T>>,
    proCardCount: Int?,
    onAction: (WhatChangedAction) -> Unit,
    readableRow: @Composable LazyItemScope.(item: T, position: ListItemPosition) -> Unit,
) {
    val firstLocked = rows.indexOfFirst { it is LockableRow.Locked }
    if (proCardCount == null || firstLocked < 0) {
        rowGroup(prefix, rows, onAction, readableRow)
        return
    }
    rowGroup(prefix, rows.subList(0, firstLocked), onAction, readableRow)
    item(key = PRO_CARD_KEY) {
        ProUnlockCard(
            lockedAppCount = proCardCount,
            onUnlock = { onAction(WhatChangedAction.OpenLocked(WhatChangedPlacement.Card)) },
            modifier = Modifier
                .animateItem()
                .padding(vertical = 12.dp),
        )
    }
    rowGroup(prefix, rows.subList(firstLocked, rows.size), onAction, readableRow)
}

private fun <T> LazyListScope.rowGroup(
    prefix: String,
    rows: List<LockableRow<T>>,
    onAction: (WhatChangedAction) -> Unit,
    readableRow: @Composable LazyItemScope.(item: T, position: ListItemPosition) -> Unit,
) {
    itemsPositioned(items = rows, key = { _, row -> "$prefix-${row.key}" }) { position, row ->
        when (row) {
            is LockableRow.Locked -> LockedRow(
                packageName = row.packageName,
                appName = row.appName,
                position = position,
                onClick = { onAction(WhatChangedAction.OpenLocked(WhatChangedPlacement.LockedRow)) },
                modifier = Modifier.animateItem(),
            )

            is LockableRow.Readable -> readableRow(row.item, position)
        }
    }
}

@Composable
private fun UninstalledAppRow(
    app: UninstalledAppItem,
    position: ListItemPosition,
    onAction: (WhatChangedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier,
    )
}

private fun LazyListScope.changeSection(
    key: String,
    @StringRes title: Int,
    items: List<LockableRow<AppChangeItem>>,
    proCardCount: Int?,
    onAction: (WhatChangedAction) -> Unit,
    uppercase: (String) -> String,
) {
    if (items.isEmpty()) return
    item(key = "$key-header") { ListSectionHeader(title = uppercase(stringResource(title)), modifier = Modifier.animateItem()) }
    lockableRows(prefix = key, rows = items, proCardCount = proCardCount, onAction = onAction) { item, position ->
        AppChangeRow(
            item = item,
            position = position,
            onClick = { onAction(WhatChangedAction.OpenChange(item)) },
            onLongClick = { copyText -> onAction(WhatChangedAction.CopyItem(item.appName, copyText)) },
            modifier = Modifier.animateItem(),
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
        item(key = "changed-header") {
            ListSectionHeader(title = uppercase(stringResource(R.string.what_changed_section_changed_apps)), modifier = Modifier.animateItem())
        }
        item(key = "changed-empty") {
            Text(
                text = stringResource(R.string.what_changed_last_change_hint, lastChangeAt.historyDate()),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.animateItem().padding(horizontal = 4.dp),
            )
        }
        return
    }
    item(key = "changed-header") {
        ListSectionHeader(title = uppercase(stringResource(R.string.what_changed_section_changed_apps)), modifier = Modifier.animateItem())
    }
    lockableRows(
        prefix = "changed",
        rows = state.changedApps,
        proCardCount = state.lockedAppCount.takeIf { state.proCardSection == WhatChangedSection.ChangedApps },
        onAction = onAction,
    ) { item, position ->
        ChangedAppRow(app = item, position = position, onAction = onAction, modifier = Modifier.animateItem())
    }
}

@Composable
private fun ChangedAppRow(
    app: ChangedAppItem,
    position: ListItemPosition,
    onAction: (WhatChangedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val netChange = app.netChange as? NetChange.Changed
    AppHistoryAppRow(
        packageName = app.packageName,
        appName = app.appName,
        supportingText = app.supportingText(),
        parts = netChange?.parts.orEmpty(),
        isWarning = netChange?.hasCertificateChange == true,
        position = position,
        onClick = { onAction(WhatChangedAction.OpenAppHistory(app.packageName, app.appName)) },
        modifier = modifier,
    )
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
        LockableRow.Readable(PreviewChange, PreviewChange.key),
        PreviewChange.copy(packageName = "com.whatsapp", appName = "WhatsApp", snapshotId = 9, parts = emptyList(), hasCertificateChange = true)
            .let { LockableRow.Readable(it, it.key) },
        LockableRow.Locked("com.torch.pro:3", "com.torch.pro", "Torch Pro"),
    ),
    changedApps = listOf(
        LockableRow.Readable(
            ChangedAppItem("com.instagram.android", "Instagram", 2, false, NetChange.Changed(PreviewChange.parts, hasCertificateChange = false)),
            "com.instagram.android",
        ),
        LockableRow.Locked("org.thoughtcrime.securesms", "org.thoughtcrime.securesms", "Signal"),
        LockableRow.Locked("com.android.chrome", "com.android.chrome", "Chrome"),
    ),
    uninstalledApps = listOf(LockableRow.Locked("com.torch.pro", "com.torch.pro", "Torch Pro")),
    lockedAppCount = 3,
    proCardSection = WhatChangedSection.NeedsAttention,
)

private val PreviewPro = PreviewLoaded.copy(
    needsAttention = PreviewLoaded.needsAttention.filterIsInstance<LockableRow.Readable<AppChangeItem>>(),
    changedApps = PreviewLoaded.changedApps.filterIsInstance<LockableRow.Readable<ChangedAppItem>>() +
        LockableRow.Readable(ChangedAppItem("org.thoughtcrime.securesms", "Signal", 0, true, NetChange.NotCompared), "org.thoughtcrime.securesms") +
        LockableRow.Readable(ChangedAppItem("com.android.chrome", "Chrome", 2, false, NetChange.None), "com.android.chrome"),
    uninstalledApps = listOf(LockableRow.Readable(UninstalledAppItem("com.torch.pro", "Torch Pro", 5), "com.torch.pro")),
    lockedAppCount = 0,
    proCardSection = null,
)

@Preview(heightDp = 1300)
@Composable
private fun WhatChangedLoadedPreview() {
    ApkAnalyzerTheme {
        WhatChangedContent(state = PreviewLoaded, onAction = {})
    }
}

@Preview(heightDp = 1300)
@Composable
private fun WhatChangedProPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        WhatChangedContent(state = PreviewPro, onAction = {})
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

@Preview
@Composable
private fun WhatChangedLoadingPreview() {
    ApkAnalyzerTheme {
        WhatChangedContent(state = WhatChangedState.Loading, onAction = {})
    }
}

@Preview
@Composable
private fun WhatChangedLoadingDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        WhatChangedContent(state = WhatChangedState.Loading, onAction = {})
    }
}
