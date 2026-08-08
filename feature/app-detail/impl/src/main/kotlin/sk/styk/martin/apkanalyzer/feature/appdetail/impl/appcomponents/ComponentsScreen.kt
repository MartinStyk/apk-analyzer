package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.collections.immutable.persistentSetOf
import sk.styk.martin.apkanalyzer.core.common.util.sendForeignBroadcast
import sk.styk.martin.apkanalyzer.core.common.util.startForeignActivity
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButtonStyle
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SelectorChip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Tag
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.TextButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeader
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.collapsingHeaderContainer
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.rememberCollapsingHeaderState
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.ListSectionHeader
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionLoading

@Composable
internal fun ComponentsScreen(
    appDetailInput: AppDetailInput,
    initialScope: ComponentScope,
    initialFilters: Set<ComponentFilter>,
    onBack: () -> Unit,
    onNavigateToIntentFilters: (componentName: String, componentType: ComponentType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComponentsViewModel = hiltViewModel { factory: ComponentsViewModel.Factory ->
        factory.create(appDetailInput, initialScope, initialFilters)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ComponentsEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.general_info_copied, Toast.LENGTH_SHORT).show()

                is ComponentsEvent.LaunchComponent -> {
                    val result = when (event.type) {
                        ComponentType.Receiver -> context.sendForeignBroadcast(event.packageName, event.className)
                        else -> context.startForeignActivity(event.packageName, event.className)
                    }
                    val message = when {
                        result.isFailure -> R.string.components_launch_failed
                        event.type == ComponentType.Receiver -> R.string.components_launch_broadcast_sent
                        else -> R.string.components_launch_started
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    ComponentsContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onNavigateToIntentFilters = onNavigateToIntentFilters,
        modifier = modifier,
    )
}

@Composable
private fun ComponentsContent(
    state: ComponentsState,
    onAction: (ComponentsAction) -> Unit,
    onBack: () -> Unit,
    onNavigateToIntentFilters: (componentName: String, componentType: ComponentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.components_title),
            onBack = onBack,
        )
        when (state) {
            is ComponentsState.Loading -> SectionLoading()

            is ComponentsState.Error -> SectionError(
                message = stringResource(R.string.components_error),
                onRetry = { onAction(ComponentsAction.Retry) },
            )

            is ComponentsState.Loaded -> LoadedContent(
                state = state,
                onAction = onAction,
                onNavigateToIntentFilters = onNavigateToIntentFilters,
            )
        }
    }
}

@Composable
private fun LoadedContent(
    state: ComponentsState.Loaded,
    onAction: (ComponentsAction) -> Unit,
    onNavigateToIntentFilters: (componentName: String, componentType: ComponentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailComponentKey by rememberSaveable { mutableStateOf<String?>(null) }
    val collapsingState = rememberCollapsingHeaderState()
    val listState = rememberLazyListState()
    val detailItem = remember(detailComponentKey, state.sections) {
        state.sections.firstNotNullOfOrNull { section -> section.components.firstOrNull { it.stableKey == detailComponentKey } }
    }

    val narrowingKey = "${state.scope}|${state.selectedFilters.joinToString(",")}|${state.query}"
    var appliedNarrowingKey by rememberSaveable { mutableStateOf(narrowingKey) }
    LaunchedEffect(narrowingKey) {
        if (appliedNarrowingKey != narrowingKey) {
            appliedNarrowingKey = narrowingKey
            listState.scrollToItem(0)
        }
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
            SearchBarActive(
                query = state.query,
                placeholder = pluralStringResource(state.scope.hintRes, state.scopeTotal, state.scopeTotal),
                onQueryChange = { onAction(ComponentsAction.ChangeQuery(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            NarrowingRow(state = state, onAction = onAction)
        }

        if (state.hasResults) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { collapsingState.contentOffset },
            ) {
                state.sections.forEach { section ->
                    if (state.isSectioned) {
                        item(key = section.type.name) {
                            ListSectionHeader(
                                title = stringResource(
                                    R.string.components_section_header,
                                    stringResource(section.type.sectionLabelRes).uppercase(),
                                    section.components.size,
                                ),
                            )
                        }
                    }
                    items(items = section.components, key = { it.stableKey }) { component ->
                        ComponentRow(
                            item = component,
                            onClick = { detailComponentKey = component.stableKey },
                            onLongClick = { onAction(ComponentsAction.CopyValue(component.simpleName, component.name)) },
                            onLaunch = { onAction(ComponentsAction.LaunchComponent(component.name, component.type)) },
                        )
                    }
                }
            }
        } else {
            EmptyContent(
                state = state,
                onAction = onAction,
                modifier = Modifier.offset { collapsingState.contentOffset },
            )
        }
    }

    detailItem?.let { item ->
        ComponentDetailBottomSheet(
            item = item,
            onCopy = { label, value -> onAction(ComponentsAction.CopyValue(label, value)) },
            onOpenIntentFilters = {
                detailComponentKey = null
                onNavigateToIntentFilters(item.name, item.type)
            },
            onDismiss = { detailComponentKey = null },
        )
    }
}

@Composable
private fun NarrowingRow(
    state: ComponentsState.Loaded,
    onAction: (ComponentsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        SelectorChip(
            sheetTitle = stringResource(R.string.components_scope_sheet_title),
            options = state.scopeOptions,
            selected = state.scope,
            label = { stringResource(it.labelRes) },
            onSelectOption = { onAction(ComponentsAction.SelectScope(it)) },
        )
        ComponentFilter.entries.forEach { filter ->
            Chip(
                label = stringResource(filter.labelRes),
                selected = filter in state.selectedFilters,
                onClick = { onAction(ComponentsAction.ToggleFilter(filter)) },
            )
        }
    }
}

@Composable
private fun ComponentRow(
    item: ComponentItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = item.type.icon,
            contentDescription = stringResource(item.type.sectionLabelRes),
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.simpleName,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.packagePath.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.packagePath,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
            val hasStatusTag = item.isUnprotected || item.isLauncher || item.isExported
            if (hasStatusTag || item.flags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.isUnprotected) {
                        Icon(
                            imageVector = ApkAnalyzerIcons.Warning,
                            contentDescription = stringResource(R.string.components_tag_unprotected),
                            tint = AppTheme.colors.warning,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (item.isLauncher) {
                        Tag(
                            text = stringResource(R.string.components_tag_launcher),
                            highlighted = true,
                        )
                    }
                    if (item.isExported) {
                        Tag(text = stringResource(R.string.components_tag_exported))
                    }
                    item.flags.forEach { flag ->
                        Tag(text = stringResource(flag.labelRes))
                    }
                }
            }
        }
        if (item.isLaunchable) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                imageVector = ApkAnalyzerIcons.PlayArrow,
                onClick = onLaunch,
                style = IconButtonStyle.Filled,
                contentDescription = stringResource(R.string.components_launch),
                iconSize = 18.dp,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun EmptyContent(
    state: ComponentsState.Loaded,
    onAction: (ComponentsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
    ) {
        Text(
            text = when {
                state.query.isNotBlank() -> stringResource(R.string.components_empty_query, state.query)
                state.isNarrowed -> stringResource(R.string.components_empty_filters)
                else -> stringResource(R.string.components_empty_scope)
            },
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        if (state.isNarrowed) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = stringResource(R.string.components_clear_narrowing),
                onClick = { onAction(ComponentsAction.ClearNarrowing) },
            )
        }
    }
}

@Preview
@Composable
private fun ComponentsLoadingPreview() {
    ApkAnalyzerTheme {
        ComponentsContent(
            state = ComponentsState.Loading,
            onAction = {},
            onBack = {},
            onNavigateToIntentFilters = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun ComponentsErrorPreview() {
    ApkAnalyzerTheme {
        ComponentsContent(
            state = ComponentsState.Error,
            onAction = {},
            onBack = {},
            onNavigateToIntentFilters = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun ComponentsLoadedPreview() {
    ApkAnalyzerTheme {
        ComponentsContent(
            state = sampleLoadedState(),
            onAction = {},
            onBack = {},
            onNavigateToIntentFilters = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun ComponentsEmptyResultPreview() {
    ApkAnalyzerTheme {
        ComponentsContent(
            state = sampleLoadedState().copy(query = "widget", sections = persistentListOf()),
            onAction = {},
            onBack = {},
            onNavigateToIntentFilters = { _, _ -> },
        )
    }
}

private fun sampleLoadedState() = ComponentsState.Loaded(
    scope = ComponentScope.All,
    scopeOptions = persistentListOf(
        ComponentScope.All,
        ComponentScope.Activities,
        ComponentScope.Services,
        ComponentScope.Providers,
    ),
    selectedFilters = persistentSetOf(),
    query = "",
    scopeTotal = 428,
    sections = persistentListOf(
        ComponentSection(
            type = ComponentType.Activity,
            components = persistentListOf(
                ComponentItem(
                    name = "com.spotify.music.features.home.HomeActivity",
                    simpleName = "HomeActivity",
                    packagePath = "com.spotify.music.features.home",
                    type = ComponentType.Activity,
                    isExported = true,
                    isGuarded = false,
                    isUnprotected = false,
                    isLaunchable = true,
                    flags = persistentListOf(),
                    details = ComponentDetails.ActivityDetails(
                        label = "Spotify",
                        targetActivity = null,
                        parentName = null,
                        permission = null,
                        isLauncher = true,
                    ),
                ),
                ComponentItem(
                    name = "com.spotify.music.features.home.HomeDetailActivity",
                    simpleName = "HomeDetailActivity",
                    packagePath = "com.spotify.music.features.home",
                    type = ComponentType.Activity,
                    isExported = false,
                    isGuarded = false,
                    isUnprotected = false,
                    isLaunchable = false,
                    flags = persistentListOf(),
                    details = ComponentDetails.ActivityDetails(
                        label = null,
                        targetActivity = null,
                        parentName = null,
                        permission = null,
                        isLauncher = false,
                    ),
                ),
            ),
        ),
        ComponentSection(
            type = ComponentType.Service,
            components = persistentListOf(
                ComponentItem(
                    name = "com.spotify.music.playback.PlaybackService",
                    simpleName = "PlaybackService",
                    packagePath = "com.spotify.music.playback",
                    type = ComponentType.Service,
                    isExported = true,
                    isGuarded = true,
                    isUnprotected = false,
                    isLaunchable = false,
                    flags = persistentListOf(ComponentFlag.IsolatedProcess),
                    details = ComponentDetails.ServiceDetails(permission = "android.permission.BIND_JOB_SERVICE"),
                ),
            ),
        ),
    ),
)
