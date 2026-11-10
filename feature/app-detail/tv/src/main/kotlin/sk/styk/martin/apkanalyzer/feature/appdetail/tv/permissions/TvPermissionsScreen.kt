package sk.styk.martin.apkanalyzer.feature.appdetail.tv.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.GrantState
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionScope
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.explanationRes
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.grantExplanationRes
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.labelRes
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.permissionIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.FilterChip
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.InfoListItem
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.rememberRestorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.focus.restorableFocus
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.iconReference
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.R
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.DetailPane
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.ListDetailScaffold
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.ListSectionHeader
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.PaneField
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionLoading
import sk.styk.martin.apkanalyzer.core.apppermissions.R as PermissionsR

@Composable
internal fun TvPermissionsScreen(
    appDetailInput: AppDetailInput,
    initialPermissionName: String?,
    viewModel: TvPermissionsViewModel = hiltViewModel { factory: TvPermissionsViewModel.Factory ->
        factory.create(appDetailInput, initialPermissionName)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TvPermissionsContent(state = state, appReference = appDetailInput.iconReference, onAction = viewModel::onAction)
}

@Composable
private fun TvPermissionsContent(
    state: TvPermissionsState,
    appReference: AppReference,
    onAction: (TvPermissionsAction) -> Unit,
) {
    when (state) {
        TvPermissionsState.Loading -> SectionLoading()

        TvPermissionsState.Error -> SectionError(
            message = stringResource(PermissionsR.string.permissions_error),
            onRetry = { onAction(TvPermissionsAction.Retry) },
        )

        is TvPermissionsState.Loaded -> LoadedContent(state = state, appReference = appReference, onAction = onAction)
    }
}

@Composable
private fun LoadedContent(
    state: TvPermissionsState.Loaded,
    appReference: AppReference,
    onAction: (TvPermissionsAction) -> Unit,
) {
    val permissions = state.sections.flatMap { it.permissions }
    var selectedName by rememberSaveable { mutableStateOf(state.initialPermissionName) }
    val selected = permissions.firstOrNull { it.name == selectedName } ?: permissions.firstOrNull()
    val focus = rememberRestorableFocus(
        availableKeys = buildSet {
            state.scopeOptions.forEach { add(scopeKey(it.scope)) }
            permissions.forEach { add(permissionKey(it.name)) }
        },
        defaultKey = permissions.firstOrNull { it.name == state.initialPermissionName }?.let { permissionKey(it.name) }
            ?: permissions.firstOrNull()?.let { permissionKey(it.name) }
            ?: scopeKey(state.scope),
    )

    ListDetailScaffold(
        title = stringResource(PermissionsR.string.permissions_title),
        subtitle = state.appName,
        appReference = appReference,
        selected = selected,
        controls = if (state.hasScopeChoice) {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.scopeOptions.forEach { option ->
                        FilterChip(
                            selected = option.scope == state.scope,
                            onClick = { onAction(TvPermissionsAction.SelectScope(option.scope)) },
                            label = stringResource(R.string.tv_label_count, stringResource(option.scope.labelRes), option.count),
                            modifier = Modifier.restorableFocus(focus, scopeKey(option.scope)),
                        )
                    }
                }
            }
        } else {
            null
        },
        detail = { item, paneModifier -> PermissionPane(item = item, modifier = paneModifier) },
    ) {
        if (permissions.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.tv_permissions_empty),
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
        }
        state.sections.forEach { section ->
            item(key = "section:${section.protectionLevel?.name ?: UNCLASSIFIED_SECTION}") {
                ListSectionHeader(
                    text = stringResource(
                        R.string.tv_label_count,
                        stringResource(section.protectionLevel.labelRes),
                        section.permissions.size,
                    ),
                )
            }
            items(section.permissions, key = { permissionKey(it.name) }) { item ->
                InfoListItem(
                    label = item.label,
                    value = item.grantState?.let { stringResource(it.labelRes) } ?: item.name,
                    onClick = {},
                    highlighted = item.isGrantedDangerous,
                    leadingIcon = permissionIcon(item.name, item.groupName),
                    modifier = Modifier.restorableFocus(focus, permissionKey(item.name)).onFocusChanged {
                        if (it.isFocused) selectedName = item.name
                    },
                )
            }
        }
    }
}

@Composable
private fun PermissionPane(item: PermissionItem, modifier: Modifier = Modifier) {
    DetailPane(
        title = item.label,
        value = item.name,
        explanation = item.description,
        icon = permissionIcon(item.name, item.groupName),
        status = stringResource(item.protectionLevel.labelRes),
        highlighted = item.isGrantedDangerous,
        isCodeValue = true,
        modifier = modifier,
    ) {
        item.grantState?.let { grantState ->
            PaneField(
                label = stringResource(PermissionsR.string.permissions_detail_grant_state),
                value = stringResource(grantState.labelRes),
                explanation = item.grantExplanationRes?.let { stringResource(it) },
                valueColor = if (item.isGrantedDangerous) AppTheme.colors.warning else AppTheme.colors.onSurface,
            )
        }
        PaneField(
            label = stringResource(PermissionsR.string.permissions_detail_protection_level),
            value = stringResource(item.protectionLevel.labelRes),
            explanation = stringResource(item.protectionLevel.explanationRes),
        )
        if (item.protectionFlags.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(PermissionsR.string.permissions_detail_extra_rules),
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.onSurfaceVariant,
                )
                item.protectionFlags.forEach { flag ->
                    Text(
                        text = stringResource(flag.explanationRes),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
        item.declaringPackage?.let {
            PaneField(label = stringResource(PermissionsR.string.permissions_detail_declared_by), value = it.value)
        }
        item.displayGroupName?.let {
            PaneField(label = stringResource(PermissionsR.string.permissions_detail_group), value = it)
        }
    }
}

private val PermissionItem.isGrantedDangerous: Boolean
    get() = protectionLevel == ProtectionLevel.Dangerous && grantState == GrantState.Granted

private fun scopeKey(scope: PermissionScope) = "scope:${scope.name}"

private fun permissionKey(name: String) = "permission:$name"

private const val UNCLASSIFIED_SECTION = "unclassified"

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvPermissionsContentPreview() {
    val camera = PermissionItem(
        name = "android.permission.CAMERA",
        label = "Camera",
        description = "Take pictures and record video.",
        groupName = "android.permission-group.CAMERA",
        protectionLevel = ProtectionLevel.Dangerous,
        protectionFlags = listOf(),
        grantState = GrantState.Granted,
        declaringPackage = PackageName("android"),
        isSelfDeclared = false,
    )
    val internet = camera.copy(
        name = "android.permission.INTERNET",
        label = "Internet",
        description = "Open network connections.",
        groupName = null,
        protectionLevel = ProtectionLevel.Normal,
        protectionFlags = listOf(ProtectionFlag.Instant),
    )
    ApkAnalyzerTvTheme {
        TvPermissionsContent(
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            state = TvPermissionsState.Loaded(
                appName = "YouTube",
                scope = PermissionScope.Requested,
                scopeOptions = listOf(
                    TvPermissionScopeOption(PermissionScope.Requested, 2),
                    TvPermissionScopeOption(PermissionScope.Defined, 1),
                ),
                sections = listOf(
                    TvPermissionSection(ProtectionLevel.Dangerous, listOf(camera)),
                    TvPermissionSection(ProtectionLevel.Normal, listOf(internet)),
                ),
                initialPermissionName = null,
            ),
            onAction = {},
        )
    }
}
