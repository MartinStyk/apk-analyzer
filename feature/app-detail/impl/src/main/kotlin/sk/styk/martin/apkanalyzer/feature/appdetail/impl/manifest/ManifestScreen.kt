package sk.styk.martin.apkanalyzer.feature.appdetail.impl.manifest

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionLoading

@Composable
internal fun ManifestScreen(
    appDetailInput: AppDetailInput,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManifestViewModel = hiltViewModel { factory: ManifestViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ManifestContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Suppress("LongMethod")
@Composable
private fun ManifestContent(
    state: ManifestState,
    onAction: (ManifestAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val expandedListState = rememberLazyListState()
    val loadedState = state as? ManifestState.Loaded
    val expand = { isExpanded = true }
    val collapse = { isExpanded = false }

    BackHandler(enabled = isExpanded) {
        collapse()
    }
    LaunchedEffect(loadedState?.query) {
        if (loadedState != null) {
            listState.scrollToItem(0)
            expandedListState.scrollToItem(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Toolbar(
                title = stringResource(R.string.manifest_title),
                onBack = onBack,
            )
            when (state) {
                ManifestState.Loading -> SectionLoading()

                ManifestState.Error -> SectionError(
                    message = stringResource(R.string.manifest_error),
                    onRetry = { onAction(ManifestAction.Retry) },
                )

                is ManifestState.Loaded -> ManifestLoadedContent(
                    state = state,
                    listState = listState,
                    onAction = onAction,
                    onExpand = expand,
                )
            }
        }
        if (loadedState != null) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(180)) +
                    scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                    ),
                exit = fadeOut(animationSpec = tween(140)) +
                    scaleOut(
                        targetScale = 0.97f,
                        animationSpec = tween(160, easing = FastOutSlowInEasing),
                    ),
            ) {
                ManifestDocumentCard(
                    state = loadedState,
                    listState = expandedListState,
                    isExpanded = true,
                    onExpand = {},
                    onCollapse = collapse,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ManifestLoadedContent(
    state: ManifestState.Loaded,
    listState: LazyListState,
    onAction: (ManifestAction) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchBarActive(
            query = state.query,
            placeholder = stringResource(R.string.manifest_search),
            onQueryChange = { onAction(ManifestAction.ChangeQuery(it)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = if (state.query.isBlank()) {
                pluralStringResource(R.plurals.manifest_line_count, state.lineCount, state.lineCount)
            } else {
                pluralStringResource(R.plurals.manifest_match_count, state.matchCount, state.matchCount, state.lineCount)
            },
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (state.additionalInstalledSplits > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.manifest_base_split_note,
                    state.additionalInstalledSplits,
                    state.additionalInstalledSplits,
                ),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ManifestDocumentCard(
            state = state,
            listState = listState,
            isExpanded = false,
            onExpand = onExpand,
            onCollapse = {},
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ManifestDocumentCard(
    state: ManifestState.Loaded,
    listState: LazyListState,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = if (isExpanded) RectangleShape else RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(AppTheme.colors.surface)
            .then(if (isExpanded) Modifier else Modifier.clickable(onClick = onExpand)),
    ) {
        if (state.displayedLines.isEmpty()) {
            Text(
                text = stringResource(R.string.manifest_no_matches, state.query),
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.displayedLines,
                    key = { it.number },
                ) { line ->
                    ManifestLineRow(line = line)
                }
            }
        }
        IconButton(
            imageVector = if (isExpanded) ApkAnalyzerIcons.FullscreenExit else ApkAnalyzerIcons.Fullscreen,
            onClick = if (isExpanded) onCollapse else onExpand,
            contentDescription = stringResource(
                if (isExpanded) R.string.manifest_collapse else R.string.manifest_expand,
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.surface),
        )
    }
}

@Composable
private fun ManifestLineRow(line: ManifestLine, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = line.number.toString(),
            style = AppTheme.typography.monospace,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier
                .width(44.dp)
                .padding(end = 8.dp),
        )
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = line.text,
                style = AppTheme.typography.monospace,
                color = if (line.isMatch) AppTheme.colors.primary else AppTheme.colors.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun ManifestLoadedPreview() {
    ApkAnalyzerTheme {
        ManifestContent(
            state = ManifestState.Loaded(
                query = "exported",
                displayedLines = kotlinx.collections.immutable.persistentListOf(
                    ManifestLine(number = 23, text = "  <activity", isMatch = false),
                    ManifestLine(number = 24, text = "    android:exported=\"true\"", isMatch = true),
                    ManifestLine(number = 50, text = "  <receiver", isMatch = false),
                    ManifestLine(number = 51, text = "    android:exported=\"false\"", isMatch = true),
                ),
                lineCount = 86,
                matchCount = 2,
                additionalInstalledSplits = 3,
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ManifestErrorPreview() {
    ApkAnalyzerTheme {
        ManifestContent(
            state = ManifestState.Error,
            onAction = {},
            onBack = {},
        )
    }
}
