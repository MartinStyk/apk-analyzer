@file:Suppress("TooManyFunctions")

package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryFieldChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryInstallSource
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryItemField
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.breakHeadlineRes
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.certificateExplanationRes
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.collapsedToTwoLines
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.installSourceLabel
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.label
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.sizeLabel
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.GrantState
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionItem
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.PermissionScope
import sk.styk.martin.apkanalyzer.core.apppermissions.detail.labelRes
import sk.styk.martin.apkanalyzer.core.apps.components.ComponentFlag
import sk.styk.martin.apkanalyzer.core.apps.installsource.rationaleRes
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryItem
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryVariant
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.apps.signing.labelRes
import sk.styk.martin.apkanalyzer.core.apps.signing.toFingerprint
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.LabeledHashBox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.OutlinedButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.card
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentDetailBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentDetails
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentItem
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentRow
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.appcomponents.ComponentType
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.InfoRow
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.InfoRowItem
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.ListSectionHeader
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.RationaleBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionCard
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionLoading
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.WarningBanner
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.nativelibraries.NativeLibraryDetailBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.nativelibraries.NativeLibraryRow
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.permissions.PermissionDetailBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.permissions.PermissionRow
import java.time.Instant
import sk.styk.martin.apkanalyzer.core.apphistory.R as HistoryR
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

private const val COLLAPSED_GROUP_SIZE = 5

@Composable
internal fun AppHistoryChangeScreen(
    packageName: String,
    snapshotId: Long,
    appName: String,
    onBack: () -> Unit,
    onNavigateToFullDetail: (AppDetailInput.Historical) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppHistoryChangeViewModel = hiltViewModel { factory: AppHistoryChangeViewModel.Factory ->
        factory.create(packageName, snapshotId)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AppHistoryChangeEvent.NavigateBack -> onBack()
                is AppHistoryChangeEvent.NavigateToFullDetail -> onNavigateToFullDetail(event.input)
                AppHistoryChangeEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.general_info_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AppHistoryChangeContent(
        state = state,
        fallbackAppName = appName,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun AppHistoryChangeContent(
    state: AppHistoryChangeState,
    fallbackAppName: String,
    onAction: (AppHistoryChangeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        val loaded = state as? AppHistoryChangeState.Loaded
        Toolbar(
            title = loaded?.appName ?: fallbackAppName,
            subtitle = loaded?.whenLine(),
            onBack = { onAction(AppHistoryChangeAction.Back) },
        )
        Box(modifier = Modifier.weight(1f)) {
            when (state) {
                AppHistoryChangeState.Loading -> SectionLoading()

                AppHistoryChangeState.Error -> SectionError(
                    message = stringResource(R.string.app_history_error),
                    onRetry = { onAction(AppHistoryChangeAction.Retry) },
                )

                AppHistoryChangeState.NotFound -> SectionError(message = stringResource(R.string.app_history_version_not_found))

                is AppHistoryChangeState.Loaded -> LoadedContent(state = state, onAction = onAction)
            }
        }
        if (loaded != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                OutlinedButton(
                    text = stringResource(R.string.app_history_full_detail),
                    onClick = { onAction(AppHistoryChangeAction.OpenFullDetail) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AppHistoryChangeState.Loaded.whenLine(): String {
    val date = timestamp.historyDate()
    val recordedDate = capturedAt.historyDate()
    val isSameDay = timestamp.isSameDayAs(capturedAt)
    return when (kind) {
        AppHistoryEntry.Change.Kind.NewDevice -> stringResource(HistoryR.string.app_history_when_recorded, recordedDate)

        AppHistoryEntry.Change.Kind.Updated, AppHistoryEntry.Change.Kind.Reinstalled -> {
            val primary = stringResource(
                if (kind == AppHistoryEntry.Change.Kind.Updated) HistoryR.string.app_history_when_updated else HistoryR.string.app_history_when_reinstalled,
                date,
            )
            if (isSameDay) primary else stringResource(HistoryR.string.app_history_when_with_recorded, primary, recordedDate)
        }
    }
}

@Composable
private fun LoadedContent(
    state: AppHistoryChangeState.Loaded,
    onAction: (AppHistoryChangeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedGroupIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedRowKey by rememberSaveable { mutableStateOf<String?>(null) }
    var rationaleRow by remember { mutableStateOf<InfoRow?>(null) }
    val onCopy: (String, String) -> Unit = { label, value -> onAction(AppHistoryChangeAction.CopyValue(label, value)) }
    val resources = LocalResources.current
    val headline = state.breakHeadlineText() ?: (state.versionChange?.label(resources) ?: state.version.label(resources))
    val versionLine = (state.versionChange?.label(resources) ?: state.version.label(resources)).takeIf { it != headline }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            ChangeHeader(state = state, headline = headline, versionLine = versionLine)
        }
        state.sections.forEach { section ->
            section(
                section = section,
                kind = state.kind,
                expandedGroupIds = expandedGroupIds,
                onExpandGroup = { expandedGroupIds = expandedGroupIds + it },
                onSelectRow = { selectedRowKey = it },
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
        }
        if (state.hasUncomparableDetails) {
            item(key = "uncomparable") {
                UncomparableFooter(state = state, onShowRationale = { rationaleRow = it }, onCopy = onCopy)
            }
        }
    }

    val selectedRow = remember(selectedRowKey, state.sections) {
        state.sections.flatMap { it.groups }.flatMap { it.rows }.firstOrNull { it.key == selectedRowKey }
    }
    selectedRow?.let { row ->
        RowSheet(row = row, onCopy = onCopy, onDismiss = { selectedRowKey = null })
    }
    rationaleRow?.let { row ->
        RationaleBottomSheet(row = row, onDismiss = { rationaleRow = null })
    }
}

private val HistoryChangeSection.groups: List<HistoryChangeGroup>
    get() = when (this) {
        is HistoryChangeSection.Permissions -> groups
        is HistoryChangeSection.Components -> groups
        is HistoryChangeSection.NativeLibraries -> groups
        else -> emptyList()
    }

@Composable
private fun AppHistoryChangeState.Loaded.breakHeadlineText(): String? = kind.breakHeadlineRes?.let { stringResource(it) }

@Composable
private fun ChangeHeader(
    state: AppHistoryChangeState.Loaded,
    headline: String,
    versionLine: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = headline,
            style = AppTheme.typography.titleLarge,
            color = AppTheme.colors.onBackground,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
        )
        versionLine?.let {
            Text(
                text = it,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        val tag = when {
            state.isOlderVersion -> HistoryR.string.app_history_older_version
            state.isSameVersionBuild -> HistoryR.string.app_history_same_version
            else -> null
        }
        tag?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Tag(text = stringResource(it), modifier = Modifier.padding(start = 4.dp))
        }
        if (state.hasNoChanges) {
            Text(
                text = stringResource(HistoryR.string.app_history_empty_diff),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp),
            )
        } else {
            Text(
                text = stringResource(HistoryR.string.app_history_changes_heading),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
            )
        }
        if (state.showsDeviceNote) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp),
            ) {
                Icon(
                    imageVector = ApkAnalyzerIcons.Info,
                    contentDescription = null,
                    tint = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(HistoryR.string.app_history_device_note),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.section(
    section: HistoryChangeSection,
    kind: AppHistoryEntry.Change.Kind,
    expandedGroupIds: List<String>,
    onExpandGroup: (String) -> Unit,
    onSelectRow: (String) -> Unit,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (String, String) -> Unit,
) {
    when (section) {
        is HistoryChangeSection.Signing -> {
            categoryHeader(key = "signing", titleRes = HistoryR.string.app_history_category_signing)
            if (section.certificatesBefore != null && section.certificatesAfter != null) {
                item(key = "certificate-banner") {
                    WarningBanner(
                        title = stringResource(HistoryR.string.app_history_certificate_changed),
                        message = stringResource(kind.certificateExplanationRes),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                if (section.certificatesBefore.isNotEmpty()) {
                    item(key = "certificates-before") {
                        CertificatesCard(
                            titleRes = HistoryR.string.app_history_before,
                            certificates = section.certificatesBefore,
                            onShowRationale = onShowRationale,
                            onCopy = onCopy,
                        )
                    }
                }
                if (section.certificatesAfter.isNotEmpty()) {
                    item(key = "certificates-after") {
                        CertificatesCard(
                            titleRes = HistoryR.string.app_history_after,
                            certificates = section.certificatesAfter,
                            onShowRationale = onShowRationale,
                            onCopy = onCopy,
                        )
                    }
                }
            }
            section.schemes?.let { schemes ->
                item(key = "signing-method") {
                    ValueRow(
                        label = stringResource(AppsR.string.certificates_scheme),
                        value = stringResource(HistoryR.string.app_history_value_change, schemes.before.schemeText(), schemes.after.schemeText()),
                        rationale = stringResource(AppsR.string.certificates_scheme_explanation),
                        onShowRationale = onShowRationale,
                        onCopy = onCopy,
                    )
                }
            }
        }

        is HistoryChangeSection.Permissions -> {
            categoryHeader(key = "permissions", titleRes = HistoryR.string.app_history_category_permissions)
            groups(section.groups, expandedGroupIds, onExpandGroup, onSelectRow, onCopy)
        }

        is HistoryChangeSection.InstallSource -> {
            categoryHeader(key = "install-source", titleRes = HistoryR.string.app_history_category_install_source)
            item(key = "install-source-row") {
                ValueRow(
                    label = stringResource(AppsR.string.general_info_install_source),
                    value = section.change.installSourceLabel(LocalResources.current),
                    rationale = stringResource(section.change.after.source.rationaleRes),
                    onShowRationale = onShowRationale,
                    onCopy = onCopy,
                )
            }
        }

        is HistoryChangeSection.AppSettings -> {
            categoryHeader(key = "settings", titleRes = HistoryR.string.app_history_category_settings)
            section.debuggable?.let { change ->
                item(key = "setting-debug") {
                    SettingRow(
                        labelRes = AppsR.string.general_info_debug_access,
                        change = change,
                        onValueRes = AppsR.string.general_info_enabled,
                        offValueRes = AppsR.string.general_info_disabled,
                        onRationaleRes = AppsR.string.general_info_rationale_debug_access_enabled,
                        offRationaleRes = AppsR.string.general_info_rationale_debug_access_disabled,
                        onShowRationale = onShowRationale,
                        onCopy = onCopy,
                    )
                }
            }
            section.backup?.let { change ->
                item(key = "setting-backup") {
                    SettingRow(
                        labelRes = AppsR.string.general_info_app_data_backup,
                        change = change,
                        onValueRes = AppsR.string.general_info_allowed,
                        offValueRes = AppsR.string.general_info_disabled,
                        onRationaleRes = AppsR.string.general_info_rationale_backup_allowed,
                        offRationaleRes = AppsR.string.general_info_rationale_backup_blocked,
                        onShowRationale = onShowRationale,
                        onCopy = onCopy,
                    )
                }
            }
            section.cleartext?.let { change ->
                item(key = "setting-cleartext") {
                    SettingRow(
                        labelRes = AppsR.string.general_info_unencrypted_traffic,
                        change = change,
                        onValueRes = AppsR.string.general_info_allowed,
                        offValueRes = AppsR.string.general_info_disabled,
                        onRationaleRes = AppsR.string.general_info_rationale_cleartext_allowed,
                        offRationaleRes = AppsR.string.general_info_rationale_cleartext_blocked,
                        onShowRationale = onShowRationale,
                        onCopy = onCopy,
                    )
                }
            }
        }

        is HistoryChangeSection.AndroidVersions -> {
            categoryHeader(key = "android-versions", titleRes = HistoryR.string.app_history_category_android_versions)
            section.minSdk?.let { change ->
                item(key = "min-sdk") {
                    SdkRow(AppsR.string.general_info_min_sdk, AppsR.string.general_info_rationale_min_sdk, change, onShowRationale, onCopy)
                }
            }
            section.targetSdk?.let { change ->
                item(key = "target-sdk") {
                    SdkRow(AppsR.string.general_info_target_sdk, AppsR.string.general_info_rationale_target_sdk, change, onShowRationale, onCopy)
                }
            }
        }

        is HistoryChangeSection.Components -> {
            categoryHeader(key = "components", titleRes = HistoryR.string.app_history_category_components)
            groups(section.groups, expandedGroupIds, onExpandGroup, onSelectRow, onCopy)
        }

        is HistoryChangeSection.NativeLibraries -> {
            categoryHeader(key = "native-libraries", titleRes = HistoryR.string.app_history_category_native)
            groups(section.groups, expandedGroupIds, onExpandGroup, onSelectRow, onCopy)
        }

        is HistoryChangeSection.Size -> {
            categoryHeader(key = "size", titleRes = HistoryR.string.app_history_category_size)
            item(key = "size-row") {
                ValueRow(
                    label = stringResource(AppsR.string.general_info_apk_size),
                    value = section.change.sizeLabel(LocalResources.current),
                    rationale = stringResource(AppsR.string.general_info_rationale_apk_size),
                    onShowRationale = onShowRationale,
                    onCopy = onCopy,
                )
            }
        }
    }
}

private fun LazyListScope.categoryHeader(key: String, titleRes: Int) {
    item(key = "category-$key") {
        Text(
            text = stringResource(titleRes),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.primary,
            modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 4.dp),
        )
    }
}

private fun LazyListScope.groups(
    groups: List<HistoryChangeGroup>,
    expandedGroupIds: List<String>,
    onExpandGroup: (String) -> Unit,
    onSelectRow: (String) -> Unit,
    onCopy: (String, String) -> Unit,
) {
    groups.forEachIndexed { index, group ->
        item(key = "group-${group.id}") {
            ListSectionHeader(
                title = group.headerText(),
                topPadding = if (index == 0) 8.dp else 20.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val isExpanded = group.id in expandedGroupIds || group.rows.size <= COLLAPSED_GROUP_SIZE
        items(
            items = if (isExpanded) group.rows else group.rows.take(COLLAPSED_GROUP_SIZE),
            key = { it.key },
        ) { row ->
            ChangeRow(row = row, onSelect = { onSelectRow(row.key) }, onCopy = onCopy)
        }
        if (!isExpanded) {
            item(key = "show-all-${group.id}") {
                TextButton(
                    text = stringResource(R.string.app_history_show_all, group.rows.size),
                    onClick = { onExpandGroup(group.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun HistoryChangeGroup.headerText(): String {
    val kindText = stringResource(
        when (kind) {
            HistoryChangeGroupKind.Added -> HistoryR.string.app_history_group_added
            HistoryChangeGroupKind.Removed -> HistoryR.string.app_history_group_removed
            HistoryChangeGroupKind.Changed -> HistoryR.string.app_history_group_changed
        },
    ).uppercase()
    return when (val scope = scope) {
        null -> stringResource(R.string.app_history_group_header, kindText, rows.size)
        else -> stringResource(R.string.app_history_group_header_scoped, stringResource(scope.labelRes).uppercase(), kindText, rows.size)
    }
}

@Composable
private fun ChangeRow(
    row: HistoryChangeRow,
    onSelect: () -> Unit,
    onCopy: (String, String) -> Unit,
) {
    val resources = LocalResources.current
    val allChangeLines = row.fieldChanges.map { it.label(resources) }
    val changeLines = allChangeLines.collapsedToTwoLines(resources)
    when (row) {
        is HistoryChangeRow.Permission -> PermissionRow(
            item = row.item,
            onClick = onSelect,
            onLongClick = { onCopy(row.item.label, (listOf(row.item.name) + allChangeLines).joinToString("\n")) },
            tag = stringResource(ProtectionLevel.Dangerous.labelRes).takeIf { row.item.protectionLevel == ProtectionLevel.Dangerous },
            changeLines = changeLines,
        )

        is HistoryChangeRow.Component -> ComponentRow(
            item = row.item,
            onClick = onSelect,
            onLongClick = { onCopy(row.item.simpleName, (listOf(row.item.name) + allChangeLines).joinToString("\n")) },
            changeLines = changeLines,
        )

        is HistoryChangeRow.NativeLibrary -> NativeLibraryRow(
            item = row.item,
            onClick = onSelect,
            onCopy = { label, _ -> onCopy(label, (listOf(row.item.name) + allChangeLines).joinToString("\n")) },
            changeLines = changeLines,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun RowSheet(
    row: HistoryChangeRow,
    onCopy: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalResources.current
    when (row) {
        is HistoryChangeRow.Permission -> PermissionDetailBottomSheet(
            item = row.item,
            onCopy = onCopy,
            onDismiss = onDismiss,
            changeLines = row.fieldChanges.map { it.label(resources) },
        )

        is HistoryChangeRow.Component -> ComponentDetailBottomSheet(
            item = row.item,
            onCopy = onCopy,
            onOpenIntentFilters = {},
            onDismiss = onDismiss,
            changeLines = row.fieldChanges.map { it.label(resources) },
            intentFiltersMissingText = stringResource(R.string.app_history_intent_filters_note),
        )

        is HistoryChangeRow.NativeLibrary -> NativeLibraryDetailBottomSheet(
            item = row.item,
            onCopy = onCopy,
            onDismiss = onDismiss,
            showDeviceSupport = false,
            changeLines = row.fieldChanges.map { it.label(resources) },
        )
    }
}

@Composable
private fun ValueRow(
    label: String,
    value: String,
    rationale: String,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (String, String) -> Unit,
) {
    InfoRowItem(
        label = label,
        value = value,
        rationale = rationale,
        onShowRationale = onShowRationale,
        onCopy = onCopy,
        modifier = Modifier
            .padding(bottom = 6.dp)
            .card(),
    )
}

@Suppress("LongParameterList")
@Composable
private fun SettingRow(
    labelRes: Int,
    change: ValueChange<Boolean>,
    onValueRes: Int,
    offValueRes: Int,
    onRationaleRes: Int,
    offRationaleRes: Int,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (String, String) -> Unit,
) {
    ValueRow(
        label = stringResource(labelRes),
        value = stringResource(
            HistoryR.string.app_history_value_change,
            stringResource(if (change.before) onValueRes else offValueRes),
            stringResource(if (change.after) onValueRes else offValueRes),
        ),
        rationale = stringResource(if (change.after) onRationaleRes else offRationaleRes),
        onShowRationale = onShowRationale,
        onCopy = onCopy,
    )
}

@Composable
private fun SdkRow(
    labelRes: Int,
    rationaleRes: Int,
    change: ValueChange<HistorySdkLevel>,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (String, String) -> Unit,
) {
    ValueRow(
        label = stringResource(labelRes),
        value = stringResource(HistoryR.string.app_history_value_change, change.before.text(), change.after.text()),
        rationale = stringResource(rationaleRes),
        onShowRationale = onShowRationale,
        onCopy = onCopy,
    )
}

@Composable
private fun HistorySdkLevel.text(): String = when {
    version != null && label != null -> stringResource(AppsR.string.app_detail_sdk_version, version, label)
    version != null -> stringResource(AppsR.string.app_detail_sdk_version_no_label, version)
    else -> stringResource(AppsR.string.app_detail_unknown)
}

@Composable
private fun List<SigningSchemeVersion>.schemeText(): String = map { stringResource(it.labelRes) }.joinToString(", ")

@Composable
private fun CertificatesCard(
    titleRes: Int,
    certificates: List<Certificate>,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (String, String) -> Unit,
) {
    SectionCard(
        title = stringResource(titleRes),
        modifier = Modifier.padding(bottom = 6.dp),
    ) {
        certificates.forEach { certificate ->
            val signerRationale = stringResource(HistoryR.string.app_history_signer_explanation)
            val organization = certificate.subject.organization?.takeIf { it.isNotBlank() }
            val name = certificate.subject.name?.takeIf { it.isNotBlank() }
            organization?.let {
                InfoRowItem(
                    label = stringResource(AppsR.string.certificates_organization),
                    value = it,
                    rationale = signerRationale,
                    onShowRationale = onShowRationale,
                    onCopy = onCopy,
                )
            }
            if (name != null || organization == null) {
                InfoRowItem(
                    label = stringResource(AppsR.string.certificates_name),
                    value = name ?: stringResource(AppsR.string.certificates_unknown_signer),
                    rationale = signerRationale,
                    onShowRationale = onShowRationale,
                    onCopy = onCopy,
                )
            }
            val hashLabel = stringResource(AppsR.string.certificates_sha256)
            val fingerprint = certificate.certificateHashSha256.toFingerprint()
            LabeledHashBox(
                label = hashLabel,
                value = fingerprint,
                onCopy = { onCopy(hashLabel, fingerprint) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun UncomparableFooter(
    state: AppHistoryChangeState.Loaded,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (String, String) -> Unit,
) {
    val categories = listOfNotNull(
        stringResource(HistoryR.string.app_history_category_native).takeIf { state.isNativeLibrariesUncomparable },
        stringResource(HistoryR.string.app_history_category_signing_method).takeIf { state.isSigningMethodUncomparable },
    ).joinToString(", ")
    ValueRow(
        label = stringResource(HistoryR.string.app_history_uncomparable_label),
        value = categories,
        rationale = stringResource(HistoryR.string.app_history_uncomparable_explanation),
        onShowRationale = onShowRationale,
        onCopy = onCopy,
    )
}

private val sampleTimestamp = Instant.ofEpochMilli(1_772_409_600_000)
private val sampleCapturedAt = Instant.ofEpochMilli(1_773_014_400_000)

private fun samplePermission(
    name: String,
    label: String,
    level: ProtectionLevel,
) = PermissionItem(
    name = name,
    label = label,
    description = null,
    groupName = null,
    protectionLevel = level,
    protectionFlags = emptyList(),
    grantState = null as GrantState?,
    declaringPackage = PackageName("android"),
    isSelfDeclared = false,
)

private fun sampleComponent(name: String) = ComponentItem(
    name = "com.spotify.music.$name",
    simpleName = name,
    packagePath = "com.spotify.music",
    type = ComponentType.Service,
    isExported = true,
    isGuarded = false,
    isUnprotected = true,
    isLaunchable = false,
    flags = emptyList<ComponentFlag>(),
    intentFilters = null,
    details = ComponentDetails.ServiceDetails(permission = null),
)

private fun sampleCertificate(hash: String) = Certificate(
    signAlgorithm = "SHA256withRSA",
    certificateHashMd5 = hash,
    certificateHashSha1 = hash,
    certificateHashSha256 = hash,
    publicKeyMd5 = hash,
    publicKeySha1 = hash,
    publicKeySha256 = hash,
    validFrom = Instant.ofEpochMilli(1_219_276_800_000),
    validUntil = Instant.ofEpochMilli(2_388_000_000_000),
    serialNumber = "1a2b3c",
    issuer = CertificatePrincipal(organization = "Spotify AB"),
    subject = CertificatePrincipal(organization = "Spotify AB"),
    trustLevel = CertificateTrustLevel.Valid,
    isSelfSigned = true,
)

private fun loadedState(
    kind: AppHistoryEntry.Change.Kind = AppHistoryEntry.Change.Kind.Updated,
    sections: List<HistoryChangeSection> = emptyList(),
    versionChange: ValueChange<AppHistoryVersion>? = ValueChange(AppHistoryVersion(31200, "312.0"), AppHistoryVersion(31500, "315.0")),
    isSigningMethodUncomparable: Boolean = false,
) = AppHistoryChangeState.Loaded(
    appName = "Spotify",
    kind = kind,
    timestamp = sampleTimestamp,
    capturedAt = sampleCapturedAt,
    version = AppHistoryVersion(31500, "315.0"),
    versionChange = versionChange,
    isOlderVersion = false,
    isSameVersionBuild = false,
    sections = sections,
    isSigningMethodUncomparable = isSigningMethodUncomparable,
    isNativeLibrariesUncomparable = false,
)

private val normalSections = listOf(
    HistoryChangeSection.Permissions(
        groups = listOf(
            HistoryChangeGroup(
                id = "requested-added",
                kind = HistoryChangeGroupKind.Added,
                scope = PermissionScope.Requested,
                rows = listOf(
                    HistoryChangeRow.Permission("requested-added:camera", samplePermission("android.permission.CAMERA", "Camera", ProtectionLevel.Dangerous)),
                    HistoryChangeRow.Permission(
                        "requested-added:internet",
                        samplePermission("android.permission.INTERNET", "Full network access", ProtectionLevel.Normal),
                    ),
                ),
            ),
        ),
    ),
    HistoryChangeSection.Components(
        groups = listOf(
            HistoryChangeGroup(
                id = "components-changed",
                kind = HistoryChangeGroupKind.Changed,
                rows = listOf(
                    HistoryChangeRow.Component(
                        key = "components-changed:SyncService",
                        item = sampleComponent("SyncService"),
                        fieldChanges = listOf(AppHistoryFieldChange.Flag(AppHistoryItemField.Exported, before = false, after = true)),
                    ),
                ),
            ),
        ),
    ),
    HistoryChangeSection.NativeLibraries(
        groups = listOf(
            HistoryChangeGroup(
                id = "native-added",
                kind = HistoryChangeGroupKind.Added,
                rows = listOf(
                    HistoryChangeRow.NativeLibrary(
                        key = "native-added:libfoo.so",
                        item = NativeLibraryItem(
                            name = "libfoo.so",
                            abis = listOf("arm64-v8a"),
                            totalSize = 2.megabytes,
                            isDeviceCompatible = true,
                            variants = listOf(NativeLibraryVariant("arm64-v8a", 2.megabytes, "base.apk")),
                        ),
                    ),
                ),
            ),
        ),
    ),
    HistoryChangeSection.Size(ValueChange(152.megabytes, 160.megabytes)),
)

@Preview
@Composable
private fun AppHistoryChangeLoadingPreview() {
    ApkAnalyzerTheme {
        AppHistoryChangeContent(state = AppHistoryChangeState.Loading, fallbackAppName = "Spotify", onAction = {})
    }
}

@Preview
@Composable
private fun AppHistoryChangeErrorPreview() {
    ApkAnalyzerTheme {
        AppHistoryChangeContent(state = AppHistoryChangeState.Error, fallbackAppName = "Spotify", onAction = {})
    }
}

@Preview
@Composable
private fun AppHistoryChangeNotFoundPreview() {
    ApkAnalyzerTheme {
        AppHistoryChangeContent(state = AppHistoryChangeState.NotFound, fallbackAppName = "Spotify", onAction = {})
    }
}

@Preview(heightDp = 1100)
@Composable
private fun AppHistoryChangeNormalPreview() {
    ApkAnalyzerTheme {
        AppHistoryChangeContent(state = loadedState(sections = normalSections), fallbackAppName = "Spotify", onAction = {})
    }
}

@Preview(heightDp = 1100)
@Composable
private fun AppHistoryChangeCertificatePreview() {
    ApkAnalyzerTheme {
        AppHistoryChangeContent(
            state = loadedState(
                sections = listOf(
                    HistoryChangeSection.Signing(
                        certificatesBefore = listOf(sampleCertificate("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")),
                        certificatesAfter = listOf(sampleCertificate("f0e9d8c7b6a5f4e3d2c1b0a9f8e7d6c5")),
                        schemes = null,
                    ),
                ),
                isSigningMethodUncomparable = true,
            ),
            fallbackAppName = "Spotify",
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AppHistoryChangeEmptyDiffPreview() {
    ApkAnalyzerTheme {
        AppHistoryChangeContent(
            state = loadedState(kind = AppHistoryEntry.Change.Kind.NewDevice, versionChange = null),
            fallbackAppName = "Spotify",
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AppHistoryChangeNewDevicePreview() {
    ApkAnalyzerTheme {
        AppHistoryChangeContent(
            state = loadedState(
                kind = AppHistoryEntry.Change.Kind.NewDevice,
                versionChange = null,
                sections = listOf(
                    HistoryChangeSection.InstallSource(
                        ValueChange(
                            AppHistoryInstallSource(AppSource.GooglePlay, PackageName("com.android.vending")),
                            AppHistoryInstallSource(AppSource.Sideloaded, PackageName("com.android.packageinstaller")),
                        ),
                    ),
                ),
            ),
            fallbackAppName = "Spotify",
            onAction = {},
        )
    }
}
