package sk.styk.martin.apkanalyzer.feature.appdetail.phone.nativelibraries

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraryItem
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibraryVariant
import sk.styk.martin.apkanalyzer.core.common.model.kilobytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.collapsingHeader
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.collapsingHeaderContainer
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.rememberCollapsingHeaderState
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.SectionErrorReason
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.recordedAt
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.AppDetailSectionScaffold
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionLoading
import java.time.Instant
import sk.styk.martin.apkanalyzer.core.apphistory.R as HistoryR
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.R as DomainR

@Composable
internal fun NativeLibrariesScreen(
    appDetailInput: AppDetailInput,
    appName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NativeLibrariesViewModel = hiltViewModel { factory: NativeLibrariesViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                NativeLibrariesEvent.NavigateBack -> onBack()
                NativeLibrariesEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.general_info_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    NativeLibrariesContent(
        state = state,
        appName = appName,
        onAction = viewModel::onAction,
        modifier = modifier,
        recordedAt = appDetailInput.recordedAt,
    )
}

@Composable
private fun NativeLibrariesContent(
    state: NativeLibrariesState,
    appName: String,
    onAction: (NativeLibrariesAction) -> Unit,
    modifier: Modifier = Modifier,
    recordedAt: Instant? = null,
) {
    AppDetailSectionScaffold(
        title = stringResource(DomainR.string.nativelibraries_title),
        appName = appName,
        onBack = { onAction(NativeLibrariesAction.Back) },
        recordedAt = recordedAt,
        modifier = modifier,
    ) {
        when (state) {
            NativeLibrariesState.Loading -> SectionLoading()

            is NativeLibrariesState.Error -> SectionError(
                reason = state.reason,
                loadFailedMessage = stringResource(DomainR.string.nativelibraries_error),
                onRetry = { onAction(NativeLibrariesAction.Retry) },
            )

            NativeLibrariesState.NotRecorded -> SectionError(message = stringResource(HistoryR.string.app_history_not_recorded))

            is NativeLibrariesState.Loaded -> NativeLibrariesLoadedContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun NativeLibrariesLoadedContent(
    state: NativeLibrariesState.Loaded,
    onAction: (NativeLibrariesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailName by rememberSaveable { mutableStateOf<String?>(null) }
    val detailItem = remember(detailName, state.items) {
        state.items.firstOrNull { it.name == detailName }
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
                text = pluralStringResource(DomainR.plurals.nativelibraries_heading, state.totalCount, state.totalCount),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Text(
                text = stringResource(R.string.nativelibraries_explanation),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            SearchBarActive(
                query = state.query,
                placeholder = pluralStringResource(R.plurals.nativelibraries_search_hint, state.totalCount, state.totalCount),
                onQueryChange = { onAction(NativeLibrariesAction.ChangeQuery(it)) },
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
                items(items = state.items, key = { it.name }) { item ->
                    NativeLibraryRow(
                        item = item,
                        onClick = { detailName = item.name },
                        onCopy = { label, value -> onAction(NativeLibrariesAction.CopyValue(label, value)) },
                    )
                }
            }
        } else {
            EmptyNativeLibraries(
                query = state.query,
                totalCount = state.totalCount,
                onClearQuery = { onAction(NativeLibrariesAction.ClearQuery) },
                modifier = Modifier.offset { collapsingState.contentOffset },
            )
        }
    }

    detailItem?.let { item ->
        NativeLibraryDetailBottomSheet(
            item = item,
            showDeviceSupport = !state.isHistorical,
            onCopy = { label, value -> onAction(NativeLibrariesAction.CopyValue(label, value)) },
            onDismiss = { detailName = null },
        )
    }
}

@Composable
private fun EmptyNativeLibraries(
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
                stringResource(DomainR.string.nativelibraries_empty)
            } else {
                stringResource(R.string.nativelibraries_empty_query, query)
            },
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        if (query.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = stringResource(R.string.nativelibraries_clear_search),
                onClick = onClearQuery,
            )
        }
    }
}

@Preview
@Composable
private fun NativeLibrariesLoadingPreview() {
    ApkAnalyzerTheme {
        NativeLibrariesContent(
            state = NativeLibrariesState.Loading,
            appName = "Spotify",
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun NativeLibrariesNotRecordedPreview() {
    ApkAnalyzerTheme {
        NativeLibrariesContent(
            state = NativeLibrariesState.NotRecorded,
            appName = "Spotify",
            onAction = {},
            recordedAt = Instant.ofEpochMilli(1_773_014_400_000),
        )
    }
}

@Preview
@Composable
private fun NativeLibrariesErrorPreview() {
    ApkAnalyzerTheme {
        NativeLibrariesContent(
            state = NativeLibrariesState.Error(SectionErrorReason.LoadFailed),
            appName = "Spotify",
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun NativeLibrariesLoadedPreview() {
    ApkAnalyzerTheme {
        NativeLibrariesContent(
            state = NativeLibrariesState.Loaded(
                query = "",
                totalCount = 3,
                items = listOf(
                    NativeLibraryItem(
                        name = "libapp.so",
                        abis = listOf("arm64-v8a", "armeabi-v7a"),
                        totalSize = 8600.kilobytes,
                        isDeviceCompatible = true,
                        variants = listOf(
                            NativeLibraryVariant("arm64-v8a", 4800.kilobytes, "base.apk"),
                            NativeLibraryVariant("armeabi-v7a", 3800.kilobytes, "base.apk"),
                        ),
                    ),
                    NativeLibraryItem(
                        name = "libcrashlytics.so",
                        abis = listOf("arm64-v8a"),
                        totalSize = 210.kilobytes,
                        isDeviceCompatible = true,
                        variants = listOf(
                            NativeLibraryVariant("arm64-v8a", 210.kilobytes, "split_config.arm64_v8a.apk"),
                        ),
                    ),
                    NativeLibraryItem(
                        name = "libx86only.so",
                        abis = listOf("x86"),
                        totalSize = 640.kilobytes,
                        isDeviceCompatible = false,
                        variants = listOf(
                            NativeLibraryVariant("x86", 640.kilobytes, "split_config.x86.apk"),
                        ),
                    ),
                ),
            ),
            appName = "Spotify",
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun NativeLibrariesEmptyPreview() {
    ApkAnalyzerTheme {
        NativeLibrariesContent(
            state = NativeLibrariesState.Loaded(
                query = "widget",
                totalCount = 3,
                items = listOf(),
            ),
            appName = "Spotify",
            onAction = {},
        )
    }
}
