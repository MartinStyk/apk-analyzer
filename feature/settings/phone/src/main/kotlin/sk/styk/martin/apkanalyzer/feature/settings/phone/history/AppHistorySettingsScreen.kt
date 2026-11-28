package sk.styk.martin.apkanalyzer.feature.settings.phone.history

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apphistory.api.capture.AppHistoryRecording
import sk.styk.martin.apkanalyzer.core.apphistory.api.storage.AppHistoryStorageUsage
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ConfirmDialog
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.InfoCard
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.InfoCardTone
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Switch
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.launcher.launchSafely
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.card
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.settings.phone.R

private const val EXPORT_MIME_TYPE = "application/octet-stream"
private val ImportMimeTypes = arrayOf("*/*")
private val RowMinHeight = 48.dp

@Composable
internal fun AppHistorySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppHistorySettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE)) { destination ->
        if (destination != null) viewModel.onAction(AppHistorySettingsAction.ExportTo(destination))
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { source ->
        if (source != null) viewModel.onAction(AppHistorySettingsAction.ImportFrom(source))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AppHistorySettingsEvent.NavigateBack -> onBack()

                is AppHistorySettingsEvent.CreateExportDocument -> exportLauncher.launchSafely(event.fileName) {
                    viewModel.onAction(AppHistorySettingsAction.DocumentPickerUnavailable)
                }

                AppHistorySettingsEvent.OpenImportDocument -> importLauncher.launchSafely(ImportMimeTypes) {
                    viewModel.onAction(AppHistorySettingsAction.DocumentPickerUnavailable)
                }

                is AppHistorySettingsEvent.ShowFeedback -> Toast.makeText(context, event.feedback.message(context), Toast.LENGTH_LONG).show()
            }
        }
    }

    AppHistorySettingsContent(state = state, onAction = viewModel::onAction, modifier = modifier)
}

@Composable
private fun AppHistorySettingsContent(
    state: AppHistorySettingsState,
    onAction: (AppHistorySettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.settings_app_history),
            onBack = { onAction(AppHistorySettingsAction.NavigateBack) },
        )
        when (state) {
            AppHistorySettingsState.Loading -> LoadingSpinner(modifier = Modifier.fillMaxWidth().padding(top = 48.dp))
            is AppHistorySettingsState.Loaded -> LoadedContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun LoadedContent(
    state: AppHistorySettingsState.Loaded,
    onAction: (AppHistorySettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RecordingSection(
            recording = state.recording,
            onRecordingChange = { onAction(AppHistorySettingsAction.SetRecording(it)) },
        )
        InfoCard(
            icon = ApkAnalyzerIcons.Lock,
            title = stringResource(R.string.app_history_settings_privacy_title),
            body = stringResource(R.string.app_history_settings_privacy_body),
        )
        StorageSection(usage = state.usage)
        TransferSection(
            transfer = state.transfer,
            onExport = { onAction(AppHistorySettingsAction.Export) },
            onImport = { onAction(AppHistorySettingsAction.Import) },
        )
    }

    if (state.isTurnOffConfirmationVisible) {
        ConfirmDialog(
            title = stringResource(R.string.app_history_settings_turn_off_title),
            message = stringResource(R.string.app_history_settings_turn_off_message),
            confirmText = stringResource(R.string.app_history_settings_turn_off_confirm),
            dismissText = stringResource(R.string.app_history_settings_turn_off_dismiss),
            onConfirm = { onAction(AppHistorySettingsAction.ConfirmTurnOff) },
            onDismiss = { onAction(AppHistorySettingsAction.DismissTurnOff) },
        )
    }
}

@Composable
private fun RecordingSection(
    recording: AppHistoryRecording,
    onRecordingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(title = stringResource(R.string.app_history_settings_recording_section), modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .card()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val toggleLabel = stringResource(R.string.app_history_settings_recording_toggle)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = RowMinHeight),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = toggleLabel,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.app_history_settings_recording_description),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = recording == AppHistoryRecording.On,
                    onCheckedChange = onRecordingChange,
                    enabled = recording != AppHistoryRecording.Unavailable,
                    modifier = Modifier.semantics { contentDescription = toggleLabel },
                )
            }
        }
        if (recording == AppHistoryRecording.Off) {
            Spacer(modifier = Modifier.height(8.dp))
            InfoCard(
                icon = ApkAnalyzerIcons.Warning,
                title = stringResource(R.string.app_history_settings_recording_off_title),
                body = stringResource(R.string.app_history_settings_recording_off_body),
                tone = InfoCardTone.Attention,
            )
        }
    }
}

@Composable
private fun StorageSection(usage: AppHistoryStorageUsage, modifier: Modifier = Modifier) {
    Section(title = stringResource(R.string.app_history_settings_storage_section), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .card()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .heightIn(min = RowMinHeight),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.app_history_settings_storage_used),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurface,
                )
                Text(
                    text = usage.description(),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
            if (usage is AppHistoryStorageUsage.Available) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = usage.size.formatted(),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun TransferSection(
    transfer: AppHistoryTransfer,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(title = stringResource(R.string.app_history_settings_transfer_section), modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .card(),
        ) {
            TransferRow(
                icon = ApkAnalyzerIcons.Export,
                title = stringResource(R.string.app_history_settings_export),
                description = stringResource(R.string.app_history_settings_export_description),
                isRunning = transfer == AppHistoryTransfer.Exporting,
                enabled = transfer == AppHistoryTransfer.Idle,
                onClick = onExport,
            )
            TransferRow(
                icon = ApkAnalyzerIcons.Folder,
                title = stringResource(R.string.app_history_settings_import),
                description = stringResource(R.string.app_history_settings_import_description),
                isRunning = transfer == AppHistoryTransfer.Importing,
                enabled = transfer == AppHistoryTransfer.Idle,
                onClick = onImport,
            )
        }
    }
}

@Composable
private fun TransferRow(
    icon: ImageVector,
    title: String,
    description: String,
    isRunning: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .heightIn(min = RowMinHeight),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurface,
            )
            Text(
                text = description,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        if (isRunning) {
            Spacer(modifier = Modifier.width(16.dp))
            LoadingSpinner(size = 20.dp, strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun AppHistoryStorageUsage.description(): String = when (this) {
    AppHistoryStorageUsage.Unavailable -> stringResource(R.string.app_history_settings_storage_unavailable)

    is AppHistoryStorageUsage.Available -> if (appCount == 0) {
        stringResource(R.string.app_history_settings_storage_empty)
    } else {
        pluralStringResource(R.plurals.app_history_settings_storage_apps, appCount, appCount)
    }
}

private fun AppHistorySettingsFeedback.message(context: Context): String = when (this) {
    AppHistorySettingsFeedback.Exported -> context.getString(R.string.app_history_settings_exported)
    AppHistorySettingsFeedback.ExportFailed -> context.getString(R.string.app_history_settings_export_failed)
    is AppHistorySettingsFeedback.Imported -> context.resources.getQuantityString(R.plurals.app_history_settings_imported, appCount, appCount)
    AppHistorySettingsFeedback.NothingNewToImport -> context.getString(R.string.app_history_settings_nothing_new)
    AppHistorySettingsFeedback.ImportFailed -> context.getString(R.string.app_history_settings_import_failed)
    AppHistorySettingsFeedback.DocumentPickerUnavailable -> context.getString(R.string.app_history_settings_picker_unavailable)
}

@Preview
@Composable
private fun AppHistorySettingsRecordingPreview() {
    ApkAnalyzerTheme {
        AppHistorySettingsContent(
            state = AppHistorySettingsState.Loaded(
                recording = AppHistoryRecording.On,
                usage = AppHistoryStorageUsage.Available(size = 4.megabytes, appCount = 213),
                transfer = AppHistoryTransfer.Idle,
                isTurnOffConfirmationVisible = false,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AppHistorySettingsOffExportingPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        AppHistorySettingsContent(
            state = AppHistorySettingsState.Loaded(
                recording = AppHistoryRecording.Off,
                usage = AppHistoryStorageUsage.Available(size = 12.megabytes, appCount = 1),
                transfer = AppHistoryTransfer.Exporting,
                isTurnOffConfirmationVisible = false,
            ),
            onAction = {},
        )
    }
}
