package sk.styk.martin.apkanalyzer.feature.appdetail.impl.splitapks

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledSplitApk
import sk.styk.martin.apkanalyzer.core.apps.model.SplitApkKind
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.TextButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeader
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeaderContainer
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.rememberCollapsingHeaderState
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionLoading

@Composable
internal fun SplitApksScreen(
    appDetailInput: AppDetailInput,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplitApksViewModel = hiltViewModel { factory: SplitApksViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SplitApksEvent.NavigateBack -> onBack()
                SplitApksEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.general_info_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SplitApksContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun SplitApksContent(
    state: SplitApksState,
    onAction: (SplitApksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.splitapks_title),
            onBack = { onAction(SplitApksAction.Back) },
        )
        when (state) {
            SplitApksState.Loading -> SectionLoading()

            SplitApksState.Error -> SectionError(
                message = stringResource(R.string.splitapks_error),
                onRetry = { onAction(SplitApksAction.Retry) },
            )

            is SplitApksState.Loaded -> SplitApksLoadedContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun SplitApksLoadedContent(
    state: SplitApksState.Loaded,
    onAction: (SplitApksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailPath by rememberSaveable { mutableStateOf<String?>(null) }
    val detailSplit = remember(detailPath, state.items) {
        state.items.firstOrNull { it.filePath == detailPath }
    }
    val collapsingState = rememberCollapsingHeaderState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.query) {
        listState.scrollToItem(0)
    }

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
            Text(
                text = pluralStringResource(R.plurals.splitapks_heading, state.totalCount, state.totalCount),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Text(
                text = stringResource(R.string.splitapks_explanation),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            SearchBarActive(
                query = state.query,
                placeholder = pluralStringResource(R.plurals.splitapks_search_hint, state.totalCount, state.totalCount),
                onQueryChange = { onAction(SplitApksAction.ChangeQuery(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (state.hasResults) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { collapsingState.contentOffset },
            ) {
                items(items = state.items, key = { it.filePath }) { split ->
                    SplitApkRow(
                        split = split,
                        onClick = { detailPath = split.filePath },
                        onCopy = { label, value -> onAction(SplitApksAction.CopyValue(label, value)) },
                    )
                }
            }
        } else {
            EmptySplitApks(
                query = state.query,
                totalCount = state.totalCount,
                onClearQuery = { onAction(SplitApksAction.ClearQuery) },
                modifier = Modifier.offset { collapsingState.contentOffset },
            )
        }
    }

    detailSplit?.let { split ->
        SplitApkDetailBottomSheet(
            split = split,
            onCopy = { label, value -> onAction(SplitApksAction.CopyValue(label, value)) },
            onDismiss = { detailPath = null },
        )
    }
}

@Composable
private fun SplitApkRow(
    split: InstalledSplitApk,
    onClick: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val copyLabel = stringResource(split.kind.labelRes)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onCopy(copyLabel, split.fileName) },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = split.kind.icon,
            contentDescription = copyLabel,
            tint = AppTheme.colors.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = split.kind.friendlyQualifier(split.qualifier),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = split.fileName,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = split.size.formatted(),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptySplitApks(
    query: String,
    totalCount: Int,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
    ) {
        Text(
            text = if (totalCount == 0 || query.isBlank()) {
                stringResource(R.string.splitapks_empty)
            } else {
                stringResource(R.string.splitapks_empty_query, query)
            },
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        if (query.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = stringResource(R.string.splitapks_clear_search),
                onClick = onClearQuery,
            )
        }
    }
}

@Preview
@Composable
private fun SplitApksLoadingPreview() {
    ApkAnalyzerTheme {
        SplitApksContent(
            state = SplitApksState.Loading,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SplitApksErrorPreview() {
    ApkAnalyzerTheme {
        SplitApksContent(
            state = SplitApksState.Error,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SplitApksLoadedPreview() {
    ApkAnalyzerTheme {
        SplitApksContent(
            state = SplitApksState.Loaded(
                query = "",
                totalCount = 4,
                items = persistentListOf(
                    InstalledSplitApk(
                        fileName = "split_config.arm64_v8a.apk",
                        filePath = "/data/app/com.spotify.music/split_config.arm64_v8a.apk",
                        size = 24.megabytes,
                        kind = SplitApkKind.Abi,
                        qualifier = "arm64-v8a",
                    ),
                    InstalledSplitApk(
                        fileName = "split_config.en.apk",
                        filePath = "/data/app/com.spotify.music/split_config.en.apk",
                        size = 2.megabytes,
                        kind = SplitApkKind.Language,
                        qualifier = "en",
                    ),
                    InstalledSplitApk(
                        fileName = "split_config.xxhdpi.apk",
                        filePath = "/data/app/com.spotify.music/split_config.xxhdpi.apk",
                        size = 6.megabytes,
                        kind = SplitApkKind.ScreenDensity,
                        qualifier = "xxhdpi",
                    ),
                    InstalledSplitApk(
                        fileName = "split_carmode.apk",
                        filePath = "/data/app/com.spotify.music/split_carmode.apk",
                        size = 3.megabytes,
                        kind = SplitApkKind.DynamicFeature,
                        qualifier = "carmode",
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SplitApksEmptyPreview() {
    ApkAnalyzerTheme {
        SplitApksContent(
            state = SplitApksState.Loaded(
                query = "widget",
                totalCount = 4,
                items = persistentListOf(),
            ),
            onAction = {},
        )
    }
}
