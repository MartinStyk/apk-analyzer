package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.timeline

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.itemsPositioned
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionLoading
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.HistoryVersionTag
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.historyDate
import java.time.Instant

@Composable
internal fun AppHistoryTimelineScreen(
    packageName: String,
    appName: String,
    onBack: () -> Unit,
    onNavigateToChange: (snapshotId: Long, appName: String) -> Unit,
    onNavigateToFullDetail: (AppDetailInput.Historical) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppHistoryTimelineViewModel = hiltViewModel { factory: AppHistoryTimelineViewModel.Factory ->
        factory.create(packageName)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AppHistoryTimelineEvent.NavigateBack -> onBack()
                is AppHistoryTimelineEvent.NavigateToChange -> onNavigateToChange(event.snapshotId, event.appName)
                is AppHistoryTimelineEvent.NavigateToFullDetail -> onNavigateToFullDetail(event.input)
                AppHistoryTimelineEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.general_info_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AppHistoryTimelineContent(
        state = state,
        fallbackAppName = appName,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun AppHistoryTimelineContent(
    state: AppHistoryTimelineState,
    fallbackAppName: String,
    onAction: (AppHistoryTimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = (state as? AppHistoryTimelineState.Loaded)?.appName ?: fallbackAppName,
            subtitle = stringResource(R.string.app_history_toolbar_subtitle),
            onBack = { onAction(AppHistoryTimelineAction.Back) },
        )
        when (state) {
            AppHistoryTimelineState.Loading -> SectionLoading()

            AppHistoryTimelineState.Error -> SectionError(
                message = stringResource(R.string.app_history_error),
                onRetry = { onAction(AppHistoryTimelineAction.Retry) },
            )

            AppHistoryTimelineState.Empty -> EmptyContent()

            is AppHistoryTimelineState.Loaded -> LoadedContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun LoadedContent(
    state: AppHistoryTimelineState.Loaded,
    onAction: (AppHistoryTimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "context") {
            Text(
                text = if (state.changeCount == 0) {
                    stringResource(R.string.app_history_context_first_only, state.recordingSince.historyDate())
                } else {
                    pluralStringResource(R.plurals.app_history_context, state.changeCount, state.recordingSince.historyDate(), state.changeCount)
                },
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp),
            )
        }
        state.runs.forEachIndexed { runIndex, run ->
            itemsPositioned(
                items = run,
                key = { _, row -> row.snapshotId },
            ) { position, row ->
                val isRunEnd = row === run.last() && runIndex != state.runs.lastIndex
                HistoryEntryRow(
                    row = row,
                    position = position,
                    onClick = { onAction(AppHistoryTimelineAction.OpenRow(row)) },
                    onLongClick = { onAction(AppHistoryTimelineAction.CopyRow(state.appName, it)) },
                    modifier = if (isRunEnd) Modifier.padding(bottom = 20.dp) else Modifier,
                )
            }
        }
        if (state.changeCount == 0) {
            item(key = "hint") {
                Text(
                    text = stringResource(R.string.app_history_first_only_hint),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp),
                )
            }
        }
        item(key = "footer") {
            Text(
                text = stringResource(R.string.app_history_footer),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 20.dp),
            )
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.History,
            contentDescription = null,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_history_empty_title),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.app_history_empty_body),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val sampleSince = Instant.ofEpochMilli(1_760_227_200_000)

private fun firstRecordedRow() = sampleHistoryRow(
    kind = AppHistoryRowKind.FirstRecorded,
    snapshotId = 1,
    versionChange = null,
    summary = emptyList(),
)

private fun loadedState(vararg runs: List<AppHistoryRow>) = AppHistoryTimelineState.Loaded(
    appName = "Spotify",
    recordingSince = sampleSince,
    changeCount = runs.sumOf { it.size } - 1,
    runs = runs.toList(),
)

@Preview
@Composable
private fun AppHistoryTimelineLoadingPreview() {
    ApkAnalyzerTheme {
        AppHistoryTimelineContent(state = AppHistoryTimelineState.Loading, fallbackAppName = "Spotify", onAction = {})
    }
}

@Preview
@Composable
private fun AppHistoryTimelineErrorPreview() {
    ApkAnalyzerTheme {
        AppHistoryTimelineContent(state = AppHistoryTimelineState.Error, fallbackAppName = "Spotify", onAction = {})
    }
}

@Preview
@Composable
private fun AppHistoryTimelineEmptyPreview() {
    ApkAnalyzerTheme {
        AppHistoryTimelineContent(state = AppHistoryTimelineState.Empty, fallbackAppName = "Spotify", onAction = {})
    }
}

@Preview
@Composable
private fun AppHistoryTimelineFirstOnlyPreview() {
    ApkAnalyzerTheme {
        AppHistoryTimelineContent(state = loadedState(listOf(firstRecordedRow())), fallbackAppName = "Spotify", onAction = {})
    }
}

@Preview(heightDp = 900)
@Composable
private fun AppHistoryTimelineNormalPreview() {
    ApkAnalyzerTheme {
        AppHistoryTimelineContent(
            state = loadedState(
                listOf(
                    sampleHistoryRow(snapshotId = 4),
                    sampleHistoryRow(
                        snapshotId = 3,
                        summary = listOf(AppHistorySummaryPart.SizeChanged(before = 150.megabytes, after = 152.megabytes)),
                        isQuiet = true,
                    ),
                    sampleHistoryRow(
                        snapshotId = 2,
                        versionChange = null,
                        summary = listOf(AppHistorySummaryPart.DebuggingChanged(isEnabled = false)),
                        versionTag = HistoryVersionTag.SameVersionBuild,
                    ),
                    firstRecordedRow(),
                ),
            ),
            fallbackAppName = "Spotify",
            onAction = {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun AppHistoryTimelineCertificatePreview() {
    ApkAnalyzerTheme {
        AppHistoryTimelineContent(
            state = loadedState(
                listOf(
                    sampleHistoryRow(
                        snapshotId = 4,
                        summary = listOf(AppHistorySummaryPart.PermissionsAdded(firstLabel = "Camera", otherCount = 0)),
                        hasCertificateChange = true,
                    ),
                    sampleHistoryRow(
                        snapshotId = 3,
                        summary = listOf(AppHistorySummaryPart.SizeChanged(before = 160.megabytes, after = 156.megabytes)),
                        isQuiet = true,
                        versionTag = HistoryVersionTag.OlderVersion,
                    ),
                    firstRecordedRow(),
                ),
            ),
            fallbackAppName = "Spotify",
            onAction = {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun AppHistoryTimelineBreaksPreview() {
    ApkAnalyzerTheme {
        AppHistoryTimelineContent(
            state = loadedState(
                listOf(
                    sampleHistoryRow(
                        snapshotId = 5,
                        summary = listOf(AppHistorySummaryPart.SizeChanged(before = 152.megabytes, after = 160.megabytes)),
                        isQuiet = true,
                    ),
                    sampleHistoryRow(
                        kind = AppHistoryRowKind.Change(AppHistoryEntry.Change.Kind.Reinstalled),
                        snapshotId = 4,
                        summary = listOf(AppHistorySummaryPart.PermissionsRemoved(count = 2)),
                    ),
                ),
                listOf(
                    sampleHistoryRow(
                        kind = AppHistoryRowKind.Change(AppHistoryEntry.Change.Kind.NewDevice),
                        snapshotId = 3,
                        versionChange = null,
                        summary = listOf(AppHistorySummaryPart.SizeChanged(before = 151.megabytes, after = 152.megabytes)),
                    ),
                ),
                listOf(firstRecordedRow()),
            ),
            fallbackAppName = "Spotify",
            onAction = {},
        )
    }
}
