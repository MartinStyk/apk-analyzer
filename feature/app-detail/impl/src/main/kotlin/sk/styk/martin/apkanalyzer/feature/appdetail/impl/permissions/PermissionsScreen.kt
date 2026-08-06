package sk.styk.martin.apkanalyzer.feature.appdetail.impl.permissions

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
import androidx.compose.foundation.shape.CircleShape
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
import kotlinx.collections.immutable.toImmutableList
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.model.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.MultiSelectorChip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SearchBarActive
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SelectorChip
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
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionLoading

private const val UNRESOLVED_SECTION_KEY = "unresolved"

@Composable
internal fun PermissionsScreen(
    appDetailInput: AppDetailInput,
    focusedPermission: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel = hiltViewModel { factory: PermissionsViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PermissionsEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.general_info_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    PermissionsContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        focusedPermission = focusedPermission,
        modifier = modifier,
    )
}

@Composable
private fun PermissionsContent(
    state: PermissionsState,
    onAction: (PermissionsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    focusedPermission: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.permissions_title),
            onBack = onBack,
        )
        when (state) {
            is PermissionsState.Loading -> SectionLoading()

            is PermissionsState.Error -> SectionError(
                message = stringResource(R.string.permissions_error),
                onRetry = { onAction(PermissionsAction.Retry) },
            )

            is PermissionsState.Loaded -> LoadedContent(state = state, onAction = onAction, focusedPermission = focusedPermission)
        }
    }
}

@Composable
private fun LoadedContent(
    state: PermissionsState.Loaded,
    onAction: (PermissionsAction) -> Unit,
    modifier: Modifier = Modifier,
    focusedPermission: String? = null,
) {
    var detailPermissionName by rememberSaveable { mutableStateOf(focusedPermission) }
    val collapsingState = rememberCollapsingHeaderState()
    val listState = rememberLazyListState()
    val detailItem = remember(detailPermissionName, state.sections) {
        state.sections.firstNotNullOfOrNull { section -> section.permissions.firstOrNull { it.name == detailPermissionName } }
    }

    val narrowingKey = "${state.scope}|${state.selectedProtectionLevels.joinToString(",")}|${state.selectedGrantStates.joinToString(",")}|${state.query}"
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
                placeholder = pluralStringResource(R.plurals.permissions_filter_hint, state.scopeTotal, state.scopeTotal),
                onQueryChange = { onAction(PermissionsAction.ChangeQuery(it)) },
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
                    item(key = section.protectionLevel?.name ?: UNRESOLVED_SECTION_KEY) {
                        SectionHeader(section = section)
                    }
                    items(items = section.permissions, key = { it.name }) { permission ->
                        PermissionRow(
                            item = permission,
                            onClick = { detailPermissionName = permission.name },
                            onLongClick = { onAction(PermissionsAction.CopyValue(permission.label, permission.name)) },
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
        PermissionDetailBottomSheet(
            item = item,
            onCopy = { label, value -> onAction(PermissionsAction.CopyValue(label, value)) },
            onDismiss = { detailPermissionName = null },
        )
    }
}

@Composable
private fun NarrowingRow(
    state: PermissionsState.Loaded,
    onAction: (PermissionsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val protectionLevelLabel = stringResource(R.string.permissions_filter_protection_level)
    val grantStateLabel = stringResource(R.string.permissions_filter_grant_state)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        if (state.hasScopeChoice) {
            SelectorChip(
                sheetTitle = stringResource(R.string.permissions_scope_sheet_title),
                options = state.scopeOptions,
                selected = state.scope,
                label = { stringResource(it.labelRes) },
                onSelectOption = { onAction(PermissionsAction.SelectScope(it)) },
            )
        }
        if (state.protectionLevelOptions.isNotEmpty()) {
            MultiSelectorChip(
                sheetTitle = protectionLevelLabel,
                defaultLabel = protectionLevelLabel,
                options = state.protectionLevelOptions,
                selected = state.selectedProtectionLevels,
                optionLabel = { stringResource(it.labelRes) },
                selectionLabel = { selectedOptions ->
                    val firstLabel = stringResource(selectedOptions.first().labelRes)
                    if (selectedOptions.size == 1) {
                        firstLabel
                    } else {
                        stringResource(
                            R.string.permissions_filter_multiple_selection,
                            firstLabel,
                            selectedOptions.size - 1,
                        )
                    }
                },
                onToggleOption = { onAction(PermissionsAction.ToggleProtectionLevel(it)) },
            )
        }
        if (state.grantStateOptions.isNotEmpty()) {
            MultiSelectorChip(
                sheetTitle = grantStateLabel,
                defaultLabel = grantStateLabel,
                options = state.grantStateOptions,
                selected = state.selectedGrantStates,
                optionLabel = { stringResource(it.labelRes) },
                selectionLabel = { selectedOptions ->
                    val firstLabel = stringResource(selectedOptions.first().labelRes)
                    if (selectedOptions.size == 1) {
                        firstLabel
                    } else {
                        stringResource(
                            R.string.permissions_filter_multiple_selection,
                            firstLabel,
                            selectedOptions.size - 1,
                        )
                    }
                },
                onToggleOption = { onAction(PermissionsAction.ToggleGrantState(it)) },
            )
        }
    }
}

@Composable
private fun SectionHeader(section: PermissionSection, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)) {
        Text(
            text = stringResource(
                R.string.permissions_section_header,
                stringResource(section.protectionLevel.labelRes).uppercase(),
                section.permissions.size,
            ),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.primary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(section.protectionLevel.explanationRes),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionRow(
    item: PermissionItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
            imageVector = item.icon,
            contentDescription = stringResource(item.protectionLevel.labelRes),
            tint = if (item.protectionLevel == ProtectionLevel.Dangerous) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.name,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
            )
        }
        item.grantState?.let { grantState ->
            Spacer(modifier = Modifier.width(8.dp))
            GrantPill(grantState = grantState)
        }
    }
}

@Composable
private fun GrantPill(grantState: GrantState, modifier: Modifier = Modifier) {
    val color = when (grantState) {
        GrantState.Granted -> AppTheme.colors.primary
        GrantState.NotGranted -> AppTheme.colors.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = stringResource(grantState.labelRes),
            style = AppTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun EmptyContent(
    state: PermissionsState.Loaded,
    onAction: (PermissionsAction) -> Unit,
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
                state.query.isNotBlank() -> stringResource(R.string.permissions_empty_query, state.query)
                state.isNarrowed -> stringResource(R.string.permissions_empty_filters)
                else -> stringResource(R.string.permissions_empty_scope)
            },
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        if (state.isNarrowed) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = stringResource(R.string.permissions_clear_narrowing),
                onClick = { onAction(PermissionsAction.ClearNarrowing) },
            )
        }
    }
}

@Preview
@Composable
private fun PermissionsLoadingPreview() {
    ApkAnalyzerTheme {
        PermissionsContent(
            state = PermissionsState.Loading,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun PermissionsErrorPreview() {
    ApkAnalyzerTheme {
        PermissionsContent(
            state = PermissionsState.Error,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun PermissionsLoadedPreview() {
    ApkAnalyzerTheme {
        PermissionsContent(
            state = sampleLoadedState(),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun PermissionsEmptyResultPreview() {
    ApkAnalyzerTheme {
        PermissionsContent(
            state = sampleLoadedState().copy(query = "bluetooth", sections = persistentListOf()),
            onAction = {},
            onBack = {},
        )
    }
}

private fun sampleLoadedState() = PermissionsState.Loaded(
    scope = PermissionScope.Requested,
    scopeOptions = persistentListOf(PermissionScope.Requested, PermissionScope.Defined),
    selectedProtectionLevels = persistentSetOf(),
    protectionLevelOptions = (ProtectionLevel.entries + null).toImmutableList(),
    selectedGrantStates = persistentSetOf(),
    grantStateOptions = GrantState.entries.toImmutableList(),
    query = "",
    scopeTotal = 32,
    sections = persistentListOf(
        PermissionSection(
            protectionLevel = ProtectionLevel.Dangerous,
            permissions = persistentListOf(
                PermissionItem(
                    name = "android.permission.CAMERA",
                    label = "Camera",
                    description = "Take pictures and record video with the device camera.",
                    groupName = "android.permission-group.CAMERA",
                    protectionLevel = ProtectionLevel.Dangerous,
                    protectionFlags = persistentListOf(),
                    grantState = GrantState.Granted,
                    declaringPackage = "android",
                    isSelfDeclared = false,
                ),
                PermissionItem(
                    name = "android.permission.ACCESS_FINE_LOCATION",
                    label = "Precise location",
                    description = "Read the device's exact location from GPS and network sources.",
                    groupName = "android.permission-group.LOCATION",
                    protectionLevel = ProtectionLevel.Dangerous,
                    protectionFlags = persistentListOf(ProtectionFlag.AppOp),
                    grantState = GrantState.NotGranted,
                    declaringPackage = "android",
                    isSelfDeclared = false,
                ),
            ),
        ),
        PermissionSection(
            protectionLevel = ProtectionLevel.Normal,
            permissions = persistentListOf(
                PermissionItem(
                    name = "android.permission.INTERNET",
                    label = "Full network access",
                    description = "Open network connections.",
                    groupName = null,
                    protectionLevel = ProtectionLevel.Normal,
                    protectionFlags = persistentListOf(),
                    grantState = GrantState.Granted,
                    declaringPackage = "android",
                    isSelfDeclared = false,
                ),
            ),
        ),
        PermissionSection(
            protectionLevel = null,
            permissions = persistentListOf(
                PermissionItem(
                    name = "com.sonymobile.home.permission.PROVIDER_INSERT_BADGE",
                    label = "Provider Insert Badge",
                    description = null,
                    groupName = null,
                    protectionLevel = null,
                    protectionFlags = persistentListOf(),
                    grantState = GrantState.NotGranted,
                    declaringPackage = null,
                    isSelfDeclared = false,
                ),
            ),
        ),
    ),
)
