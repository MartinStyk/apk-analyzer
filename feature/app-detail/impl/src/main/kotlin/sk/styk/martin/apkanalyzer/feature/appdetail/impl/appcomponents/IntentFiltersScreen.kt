package sk.styk.martin.apkanalyzer.feature.appdetail.impl.appcomponents

import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
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
import sk.styk.martin.apkanalyzer.core.apps.model.IntentFilterDataRuleType
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.TextButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionLoading

@Composable
internal fun IntentFiltersScreen(
    appDetailInput: AppDetailInput,
    componentName: String,
    componentType: ComponentType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IntentFiltersViewModel = hiltViewModel { factory: IntentFiltersViewModel.Factory ->
        factory.create(appDetailInput, componentName, componentType)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                IntentFiltersEvent.NavigateBack -> onBack()
                IntentFiltersEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.general_info_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    IntentFiltersContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun IntentFiltersContent(
    state: IntentFiltersState,
    onAction: (IntentFiltersAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.intent_filters_title),
            onBack = { onAction(IntentFiltersAction.Back) },
        )
        when (state) {
            IntentFiltersState.Loading -> SectionLoading()

            IntentFiltersState.Error -> SectionError(
                message = stringResource(R.string.intent_filters_error),
                onRetry = { onAction(IntentFiltersAction.Retry) },
            )

            IntentFiltersState.Unavailable -> SectionError(
                message = stringResource(R.string.components_detail_intent_filters_unavailable),
            )

            is IntentFiltersState.Loaded -> IntentFiltersLoadedContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun IntentFiltersLoadedContent(
    state: IntentFiltersState.Loaded,
    onAction: (IntentFiltersAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailFilterIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val detailFilter = remember(detailFilterIndex, state.filters) {
        state.filters.firstOrNull { it.index == detailFilterIndex }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(state.query) {
        listState.scrollToItem(0)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.intent_filters_component_heading, state.simpleComponentName),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Text(
            text = stringResource(R.string.intent_filters_explanation),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        SearchBarActive(
            query = state.query,
            placeholder = pluralStringResource(
                R.plurals.intent_filters_search_hint,
                state.totalCount,
                state.totalCount,
            ),
            onQueryChange = { onAction(IntentFiltersAction.ChangeQuery(it)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (state.hasResults) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = state.filters, key = { it.index }) { filter ->
                    IntentFilterRow(
                        filter = filter,
                        componentType = state.componentType,
                        onClick = { detailFilterIndex = filter.index },
                        onCopy = { label, value ->
                            onAction(IntentFiltersAction.CopyValue(label = label, value = value))
                        },
                    )
                }
            }
        } else {
            EmptyIntentFilters(
                query = state.query,
                onClearQuery = { onAction(IntentFiltersAction.ClearQuery) },
            )
        }
    }

    detailFilter?.let { filter ->
        IntentFilterDetailBottomSheet(
            filter = filter,
            componentType = state.componentType,
            onCopy = { label, value -> onAction(IntentFiltersAction.CopyValue(label, value)) },
            onDismiss = { detailFilterIndex = null },
        )
    }
}

@Composable
private fun IntentFilterRow(
    filter: ComponentIntentFilterItem,
    componentType: ComponentType,
    onClick: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(filter.purposeTitleRes(componentType))
    val dataRules = filter.dataRules + filter.uriRelativeGroups.filter { it.isAllowed }.flatMap { it.dataRules }
    val actionBadges = filter.actions.map { it.toDisplayRequestType() }.filter { it.isNotBlank() }
    val categoryBadges = filter.categories
        .filterNot { it == Intent.CATEGORY_DEFAULT }
        .map { it.toDisplayCategory() }
        .filter { it.isNotBlank() }
    val hasBlockedRules = filter.uriRelativeGroups.any { !it.isAllowed }
    val hasRequestBadges = actionBadges.isNotEmpty() || categoryBadges.isNotEmpty()
    val hasFilterBadges = filter.isAutoVerify || hasBlockedRules
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onCopy(title, filter.copyValue()) },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.onBackground,
        )
        IntentFilterDataField(
            label = stringResource(R.string.intent_filters_scheme),
            values = dataRules.valuesFor(IntentFilterDataRuleType.Scheme),
        )
        IntentFilterDataField(
            label = stringResource(R.string.components_intent_data_host),
            values = dataRules.valuesFor(IntentFilterDataRuleType.Host),
        )
        IntentFilterDataField(
            label = stringResource(R.string.intent_filters_path),
            values = dataRules.valuesFor(
                IntentFilterDataRuleType.Path,
                IntentFilterDataRuleType.PathPrefix,
                IntentFilterDataRuleType.PathSuffix,
                IntentFilterDataRuleType.PathPattern,
                IntentFilterDataRuleType.PathAdvancedPattern,
            ),
        )
        IntentFilterDataField(
            label = stringResource(R.string.components_intent_data_mime_type),
            values = dataRules.valuesFor(IntentFilterDataRuleType.MimeType),
        )
        if (hasRequestBadges || hasFilterBadges) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                actionBadges.forEach { action ->
                    IntentFilterTag(text = action, emphasized = true)
                }
                categoryBadges.forEach { category ->
                    IntentFilterTag(text = category)
                }
                if (filter.isAutoVerify) {
                    IntentFilterTag(text = stringResource(R.string.intent_filters_tag_verified))
                }
                if (hasBlockedRules) {
                    IntentFilterTag(text = stringResource(R.string.intent_filters_tag_blocked_rules))
                }
            }
        }
    }
}

@Composable
private fun IntentFilterDataField(
    label: String,
    values: List<String>,
    modifier: Modifier = Modifier,
) {
    if (values.isNotEmpty()) {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label.uppercase(),
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = values.joinToString(separator = "\n"),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun IntentFilterTag(
    text: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Text(
        text = text,
        style = AppTheme.typography.labelSmall,
        color = if (emphasized) AppTheme.colors.onSecondaryContainer else AppTheme.colors.onSurfaceVariant,
        modifier = modifier
            .clip(Shapes.CardShape)
            .background(if (emphasized) AppTheme.colors.secondaryContainer else AppTheme.colors.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun EmptyIntentFilters(
    query: String,
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
            text = if (query.isBlank()) {
                stringResource(R.string.intent_filters_empty)
            } else {
                stringResource(R.string.intent_filters_empty_query, query)
            },
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        if (query.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = stringResource(R.string.intent_filters_clear_search),
                onClick = onClearQuery,
            )
        }
    }
}

private fun List<IntentFilterDataRuleItem>.valuesFor(vararg types: IntentFilterDataRuleType): List<String> = filter { it.type in types }.map { it.value }.distinct()

private fun ComponentIntentFilterItem.copyValue(): String = buildList {
    addAll(actions)
    addAll(categories)
    addAll(dataRules.map { "${it.type.manifestAttribute}=${it.value}" })
    uriRelativeGroups.forEach { group ->
        add(if (group.isAllowed) "allow" else "block")
        addAll(group.dataRules.map { "${it.type.manifestAttribute}=${it.value}" })
    }
}.joinToString(separator = "\n")

@StringRes
internal fun ComponentIntentFilterItem.purposeTitleRes(componentType: ComponentType): Int {
    if (actions.isEmpty()) {
        return R.string.intent_filters_filter_fallback
    }
    val resolvedTitles = actions.map { action -> purposeTitleRes(componentType, action) }.distinct()
    return resolvedTitles.singleOrNull() ?: R.string.intent_filters_purpose_multiple
}

@StringRes
private fun ComponentIntentFilterItem.purposeTitleRes(componentType: ComponentType, action: String): Int = when (componentType) {
    ComponentType.Activity -> when (action) {
        Intent.ACTION_MAIN -> when {
            Intent.CATEGORY_LAUNCHER in categories -> R.string.intent_filters_purpose_launcher
            Intent.CATEGORY_LEANBACK_LAUNCHER in categories -> R.string.intent_filters_purpose_tv_launcher
            else -> R.string.intent_filters_purpose_main
        }

        Intent.ACTION_VIEW -> R.string.intent_filters_purpose_view

        Intent.ACTION_SEND,
        Intent.ACTION_SEND_MULTIPLE,
        -> R.string.intent_filters_purpose_share

        Intent.ACTION_SENDTO -> R.string.intent_filters_purpose_send_to

        Intent.ACTION_EDIT -> R.string.intent_filters_purpose_edit

        Intent.ACTION_PICK,
        Intent.ACTION_GET_CONTENT,
        -> R.string.intent_filters_purpose_choose_content

        Intent.ACTION_SEARCH,
        Intent.ACTION_WEB_SEARCH,
        -> R.string.intent_filters_purpose_search

        Intent.ACTION_DIAL,
        Intent.ACTION_CALL,
        -> R.string.intent_filters_purpose_call

        else -> R.string.intent_filters_purpose_activity_other
    }

    ComponentType.Receiver -> when (action) {
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_LOCKED_BOOT_COMPLETED,
        -> R.string.intent_filters_purpose_device_start

        Intent.ACTION_PACKAGE_ADDED,
        Intent.ACTION_PACKAGE_REMOVED,
        Intent.ACTION_PACKAGE_CHANGED,
        Intent.ACTION_PACKAGE_REPLACED,
        Intent.ACTION_PACKAGE_RESTARTED,
        -> R.string.intent_filters_purpose_app_change

        Intent.ACTION_POWER_CONNECTED,
        Intent.ACTION_POWER_DISCONNECTED,
        -> R.string.intent_filters_purpose_power

        else -> R.string.intent_filters_purpose_receiver_other
    }

    ComponentType.Service -> R.string.intent_filters_purpose_service_other

    ComponentType.Provider -> R.string.intent_filters_purpose_provider_other
}

@Preview
@Composable
private fun IntentFiltersLoadedPreview() {
    ApkAnalyzerTheme {
        IntentFiltersContent(
            state = IntentFiltersState.Loaded(
                componentName = "com.spotify.music.features.links.DeepLinkActivity",
                componentType = ComponentType.Activity,
                query = "",
                totalCount = 2,
                filters = persistentListOf(sampleIntentFilter()),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun IntentFiltersEmptyPreview() {
    ApkAnalyzerTheme {
        IntentFiltersContent(
            state = IntentFiltersState.Loaded(
                componentName = "com.spotify.music.features.links.DeepLinkActivity",
                componentType = ComponentType.Activity,
                query = "camera",
                totalCount = 2,
                filters = persistentListOf(),
            ),
            onAction = {},
        )
    }
}

private fun sampleIntentFilter() = ComponentIntentFilterItem(
    index = 0,
    actions = persistentListOf("android.intent.action.VIEW"),
    categories = persistentListOf(
        "android.intent.category.DEFAULT",
        "android.intent.category.BROWSABLE",
    ),
    dataRules = persistentListOf(
        IntentFilterDataRuleItem(IntentFilterDataRuleType.Scheme, "https"),
        IntentFilterDataRuleItem(IntentFilterDataRuleType.Host, "open.spotify.com"),
    ),
    uriRelativeGroups = persistentListOf(),
    priority = 0,
    order = 0,
    isAutoVerify = true,
)
