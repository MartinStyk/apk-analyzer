package sk.styk.martin.apkanalyzer.feature.appdetail.tv.appcomponents

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentFlag
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentIntentFilter
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.components.IntentFilterDataRule
import sk.styk.martin.apkanalyzer.core.apps.components.IntentFilterDataRuleType
import sk.styk.martin.apkanalyzer.core.apps.components.explanationRes
import sk.styk.martin.apkanalyzer.core.apps.components.exportedExplanationRes
import sk.styk.martin.apkanalyzer.core.apps.components.labelRes
import sk.styk.martin.apkanalyzer.core.apps.components.sectionLabelRes
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.util.sendForeignBroadcast
import sk.styk.martin.apkanalyzer.core.common.util.startForeignActivity
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.FilterChip
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.InfoListItem
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.RestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.rememberRestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.restorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.bleedHorizontally
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.iconReference
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.R
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.DetailPane
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.ListDetailScaffold
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.ListSectionHeader
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.PaneField
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.PaneSection
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionLoading
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Composable
internal fun TvComponentsScreen(
    appDetailInput: AppDetailInput,
    viewModel: TvComponentsViewModel = hiltViewModel { factory: TvComponentsViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TvComponentsEvent.LaunchComponent -> {
                    val result = when (event.kind) {
                        ComponentKind.Receiver -> context.sendForeignBroadcast(event.packageName, event.className)

                        ComponentKind.Activity, ComponentKind.Service, ComponentKind.Provider -> context.startForeignActivity(
                            event.packageName,
                            event.className,
                        )
                    }
                    val message = when {
                        result.isFailure -> AppsR.string.components_launch_failed
                        event.kind == ComponentKind.Receiver -> AppsR.string.components_launch_broadcast_sent
                        else -> AppsR.string.components_launch_started
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    TvComponentsContent(state = state, appReference = appDetailInput.iconReference, onAction = viewModel::onAction)
}

@Composable
private fun TvComponentsContent(
    state: TvComponentsState,
    appReference: AppReference,
    onAction: (TvComponentsAction) -> Unit,
) {
    when (state) {
        TvComponentsState.Loading -> SectionLoading()

        TvComponentsState.Error -> SectionError(
            message = stringResource(AppsR.string.components_error),
            onRetry = { onAction(TvComponentsAction.Retry) },
        )

        is TvComponentsState.Loaded -> LoadedContent(state = state, appReference = appReference, onAction = onAction)
    }
}

@Composable
private fun LoadedContent(
    state: TvComponentsState.Loaded,
    appReference: AppReference,
    onAction: (TvComponentsAction) -> Unit,
) {
    val components = state.sections.flatMap { it.components }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = components.firstOrNull { it.key == selectedKey } ?: components.firstOrNull()
    val focus = rememberRestorableFocus(
        availableKeys = buildSet {
            state.scopeOptions.forEach { add(scopeKey(it.scope)) }
            components.forEach { add(it.key) }
        },
        defaultKey = components.firstOrNull()?.key ?: scopeKey(state.scope),
    )

    ListDetailScaffold(
        title = stringResource(AppsR.string.components_title),
        subtitle = state.appName,
        appReference = appReference,
        selected = selected,
        controls = { ScopeChips(state = state, focus = focus, onAction = onAction) },
        detail = { item, paneModifier -> ComponentPane(item = item, modifier = paneModifier) },
    ) {
        if (components.isEmpty()) {
            item(key = "empty") {
                Text(text = stringResource(R.string.tv_components_no_components), color = AppTheme.colors.onSurfaceVariant)
            }
        }
        state.sections.forEach { section ->
            item(key = "section:${section.kind.name}") {
                ListSectionHeader(
                    text = stringResource(R.string.tv_label_count, stringResource(section.kind.sectionLabelRes), section.components.size),
                )
            }
            items(section.components, key = { it.key }) { item ->
                InfoListItem(
                    label = item.simpleName,
                    value = item.summary(),
                    onClick = { if (item.isLaunchable) onAction(TvComponentsAction.LaunchComponent(item.name, item.kind)) },
                    highlighted = item.isUnprotected,
                    leadingIcon = item.kind.icon,
                    trailingIcon = if (item.isLaunchable) ApkAnalyzerIcons.PlayArrow else null,
                    modifier = Modifier.restorableFocus(focus, item.key).onFocusChanged {
                        if (it.isFocused) selectedKey = item.key
                    },
                )
            }
        }
    }
}

@Composable
private fun ScopeChips(
    state: TvComponentsState.Loaded,
    focus: RestorableFocus,
    onAction: (TvComponentsAction) -> Unit,
) {
    LazyRow(
        modifier = Modifier.bleedHorizontally(ChipBleed),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = ChipBleed, vertical = 4.dp),
    ) {
        items(state.scopeOptions, key = { scopeKey(it.scope) }) { option ->
            FilterChip(
                selected = option.scope == state.scope,
                onClick = { onAction(TvComponentsAction.SelectScope(option.scope)) },
                label = stringResource(R.string.tv_label_count, option.scope.label(), option.count),
                modifier = Modifier.restorableFocus(focus, scopeKey(option.scope)),
            )
        }
    }
}

@Composable
private fun ComponentPane(item: TvComponentItem, modifier: Modifier = Modifier) {
    DetailPane(
        title = item.simpleName,
        value = item.name,
        icon = item.kind.icon,
        status = item.summary(),
        highlighted = item.isUnprotected,
        isCodeValue = true,
        modifier = modifier,
    ) {
        if (item.isLaunchable) LaunchHint()
        PaneSection(title = stringResource(AppsR.string.components_detail_section_reachability)) {
            PaneField(
                label = stringResource(AppsR.string.components_detail_exported),
                value = stringResource(if (item.isExported) AppsR.string.components_exported_yes else AppsR.string.components_exported_no),
                explanation = stringResource(exportedExplanationRes(item.isExported, item.isLauncher, item.isUnprotected)),
                valueColor = if (item.isUnprotected) AppTheme.colors.warning else AppTheme.colors.onSurface,
            )
            item.permission?.let { PaneField(label = stringResource(AppsR.string.components_detail_permission), value = it) }
            item.readPermission?.let { PaneField(label = stringResource(AppsR.string.components_detail_read_permission), value = it) }
            item.writePermission?.let { PaneField(label = stringResource(AppsR.string.components_detail_write_permission), value = it) }
        }
        if (item.pathPermissions.isNotEmpty()) {
            PaneSection(
                title = stringResource(AppsR.string.components_detail_section_path_permissions),
                explanation = stringResource(AppsR.string.components_detail_path_permissions_explanation),
            ) {
                val open = stringResource(AppsR.string.components_detail_path_permission_open)
                item.pathPermissions.forEach { pathPermission ->
                    PaneField(
                        label = stringResource(AppsR.string.components_detail_path_permission_path),
                        value = pathPermission.path,
                        explanation = stringResource(pathPermission.matchType.explanationRes),
                    )
                    PaneField(label = stringResource(AppsR.string.components_detail_read_permission), value = pathPermission.readPermission ?: open)
                    PaneField(label = stringResource(AppsR.string.components_detail_write_permission), value = pathPermission.writePermission ?: open)
                }
            }
        }
        IntentFiltersSection(intentFilters = item.intentFilters, kind = item.kind)
        if (item.hasTypeDetails) {
            PaneSection(title = stringResource(AppsR.string.components_detail_section_details)) {
                if (item.isLauncher) {
                    PaneField(
                        label = stringResource(AppsR.string.components_detail_launcher),
                        value = stringResource(AppsR.string.components_flag_enabled),
                        explanation = stringResource(AppsR.string.components_detail_launcher_explanation),
                    )
                }
                item.label?.let { PaneField(label = stringResource(AppsR.string.components_detail_label), value = it) }
                item.targetActivity?.let { PaneField(label = stringResource(AppsR.string.components_detail_target_activity), value = it) }
                item.parentName?.let { PaneField(label = stringResource(AppsR.string.components_detail_parent), value = it) }
                item.authority?.let { PaneField(label = stringResource(AppsR.string.components_detail_authority), value = it) }
                item.flags.forEach { flag ->
                    PaneField(
                        label = stringResource(flag.labelRes),
                        value = stringResource(AppsR.string.components_flag_enabled),
                        explanation = stringResource(flag.explanationRes),
                    )
                }
            }
        }
    }
}

@Composable
private fun LaunchHint() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(imageVector = ApkAnalyzerIcons.PlayArrow, tint = AppTheme.colors.primary, modifier = Modifier.size(24.dp))
        Text(text = stringResource(R.string.tv_components_launch_hint), style = AppTheme.typography.titleSmall, color = AppTheme.colors.primary)
    }
}

@Composable
private fun IntentFiltersSection(intentFilters: List<ComponentIntentFilter>?, kind: ComponentKind) {
    when {
        intentFilters == null -> PaneSection(title = stringResource(AppsR.string.components_detail_section_intent_filters)) {
            Text(
                text = stringResource(AppsR.string.components_detail_intent_filters_unavailable),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }

        intentFilters.isNotEmpty() -> PaneSection(
            title = stringResource(AppsR.string.components_detail_section_intent_filters),
            explanation = stringResource(AppsR.string.components_detail_intent_filters_explanation),
        ) {
            intentFilters.forEachIndexed { index, filter ->
                IntentFilterBlock(index = index, filter = filter, kind = kind)
            }
        }
    }
}

@Composable
private fun IntentFilterBlock(
    index: Int,
    filter: ComponentIntentFilter,
    kind: ComponentKind,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.tv_components_intent_filter_title, index + 1), style = AppTheme.typography.titleSmall)
        if (filter.actions.isNotEmpty()) {
            PaneField(
                label = stringResource(AppsR.string.components_detail_intent_actions),
                value = filter.actions.joinToString(separator = "\n") { it.removePrefix(FRAMEWORK_ACTION_PREFIX) },
            )
        }
        if (filter.categories.isNotEmpty()) {
            PaneField(
                label = stringResource(AppsR.string.components_detail_intent_categories),
                value = filter.categories.joinToString(separator = "\n") { it.removePrefix(FRAMEWORK_CATEGORY_PREFIX) },
            )
        }
        if (filter.dataRules.isNotEmpty()) {
            PaneField(label = stringResource(AppsR.string.components_detail_intent_data), value = dataRulesText(filter.dataRules))
        }
        filter.uriRelativeGroups.forEach { group ->
            PaneField(
                label = stringResource(
                    if (group.isAllowed) AppsR.string.components_detail_intent_allowed_uri_parts else AppsR.string.components_detail_intent_blocked_uri_parts,
                ),
                value = dataRulesText(group.dataRules),
            )
        }
        if (filter.priority != 0) {
            PaneField(
                label = stringResource(
                    when (kind) {
                        ComponentKind.Activity -> AppsR.string.components_detail_intent_activity_priority
                        ComponentKind.Receiver -> AppsR.string.components_detail_intent_receiver_priority
                        ComponentKind.Service, ComponentKind.Provider -> AppsR.string.components_detail_intent_declared_priority
                    },
                ),
                value = filter.priority.toString(),
            )
        }
        if (filter.order != 0) {
            PaneField(label = stringResource(AppsR.string.components_detail_intent_order), value = filter.order.toString())
        }
        if (filter.isAutoVerify) {
            PaneField(
                label = stringResource(AppsR.string.components_detail_intent_auto_verify),
                value = stringResource(AppsR.string.components_detail_intent_auto_verify_requested),
                explanation = stringResource(AppsR.string.components_detail_intent_auto_verify_explanation),
            )
        }
    }
}

@Composable
private fun dataRulesText(rules: List<IntentFilterDataRule>): String {
    val resources = LocalResources.current
    return rules.joinToString(separator = "\n") { rule ->
        resources.getString(AppsR.string.components_detail_intent_data_rule, resources.getString(rule.type.labelRes), rule.value)
    }
}

@Composable
private fun TvComponentItem.summary(): String = when {
    isLauncher -> stringResource(AppsR.string.components_tag_launcher)
    isUnprotected -> stringResource(AppsR.string.components_tag_unprotected)
    isExported -> stringResource(AppsR.string.components_tag_exported)
    else -> name.substringBeforeLast('.', missingDelimiterValue = name)
}

@Composable
private fun TvComponentScope.label(): String = when (this) {
    TvComponentScope.All -> stringResource(R.string.tv_components_scope_all)
    is TvComponentScope.Only -> stringResource(kind.sectionLabelRes)
}

private val TvComponentItem.hasTypeDetails: Boolean
    get() = isLauncher || label != null || targetActivity != null || parentName != null || authority != null || flags.isNotEmpty()

private fun scopeKey(scope: TvComponentScope) = when (scope) {
    TvComponentScope.All -> "scope:all"
    is TvComponentScope.Only -> "scope:${scope.kind.name}"
}

private const val FRAMEWORK_ACTION_PREFIX = "android.intent.action."
private const val FRAMEWORK_CATEGORY_PREFIX = "android.intent.category."
private val ChipBleed = 16.dp

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvComponentsContentPreview() {
    val launcher = TvComponentItem(
        name = "com.google.android.youtube.tv.MainActivity",
        kind = ComponentKind.Activity,
        isExported = true,
        isUnprotected = false,
        isLauncher = true,
        isLaunchable = true,
        intentFilters = listOf(
            ComponentIntentFilter(
                actions = listOf("android.intent.action.MAIN"),
                categories = listOf("android.intent.category.LEANBACK_LAUNCHER"),
                dataRules = listOf(IntentFilterDataRule(IntentFilterDataRuleType.Scheme, "https")),
                uriRelativeGroups = listOf(),
                priority = 0,
                order = 0,
                isAutoVerify = false,
            ),
        ),
    )
    val service = TvComponentItem(
        name = "com.google.android.youtube.tv.PlaybackService",
        kind = ComponentKind.Service,
        isExported = true,
        isUnprotected = true,
        isLauncher = false,
        flags = listOf(ComponentFlag.StopWithTask),
    )
    ApkAnalyzerTvTheme {
        TvComponentsContent(
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            state = TvComponentsState.Loaded(
                appName = "YouTube",
                scope = TvComponentScope.All,
                scopeOptions = listOf(
                    TvComponentScopeOption(TvComponentScope.All, 2),
                    TvComponentScopeOption(TvComponentScope.Only(ComponentKind.Activity), 1),
                    TvComponentScopeOption(TvComponentScope.Only(ComponentKind.Service), 1),
                ),
                sections = listOf(
                    TvComponentSection(ComponentKind.Activity, listOf(launcher)),
                    TvComponentSection(ComponentKind.Service, listOf(service)),
                ),
            ),
            onAction = {},
        )
    }
}
