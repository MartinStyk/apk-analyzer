package sk.styk.martin.apkanalyzer.feature.appdetail.impl.generalinfo

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

@Composable
internal fun GeneralInfoScreen(
    appDetailInput: AppDetailInput,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GeneralInfoViewModel = hiltViewModel { factory: GeneralInfoViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                GeneralInfoEvent.ShowCopiedFeedback -> Toast.makeText(context, R.string.general_info_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    GeneralInfoContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun GeneralInfoContent(
    state: GeneralInfoState,
    onAction: (GeneralInfoAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.general_info_title),
            onBack = onBack,
        )
        when (state) {
            is GeneralInfoState.Loading -> LoadingContent()
            is GeneralInfoState.Error -> ErrorContent()
            is GeneralInfoState.Loaded -> LoadedContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LoadingSpinner()
    }
}

@Composable
private fun ErrorContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.general_info_error),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onBackground,
        )
    }
}

@Composable
private fun LoadedContent(
    state: GeneralInfoState.Loaded,
    onAction: (GeneralInfoAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rationaleRow by remember { mutableStateOf<InfoRow?>(null) }
    val onCopy: (String, String) -> Unit = { label, value -> onAction(GeneralInfoAction.CopyValue(label, value)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoSection(title = stringResource(R.string.general_info_section_identification)) {
            InfoRowItem(
                label = stringResource(R.string.general_info_application_name),
                value = state.applicationName,
                rationale = stringResource(R.string.general_info_rationale_application_name),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            InfoRowItem(
                label = stringResource(R.string.general_info_package_name),
                value = state.packageName,
                rationale = stringResource(R.string.general_info_rationale_package_name),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            state.processName?.let { value ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_process_name),
                    value = value,
                    rationale = stringResource(R.string.general_info_rationale_process_name),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.uid?.let { value ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_uid),
                    value = value.toString(),
                    rationale = stringResource(R.string.general_info_rationale_uid),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.description?.let { value ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_description),
                    value = value,
                    rationale = stringResource(R.string.general_info_rationale_description),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
        }

        InfoSection(title = stringResource(R.string.general_info_section_versioning)) {
            state.versionName?.let { value ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_version_name),
                    value = value,
                    rationale = stringResource(R.string.general_info_rationale_version_name),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            InfoRowItem(
                label = stringResource(R.string.general_info_version_code),
                value = state.versionCode.toString(),
                rationale = stringResource(R.string.general_info_rationale_version_code),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
        }

        InfoSection(title = stringResource(R.string.general_info_section_api_targets)) {
            state.minSdkVersion?.let { version ->
                val value = if (state.minSdkLabel != null) {
                    stringResource(R.string.app_detail_sdk_version, version, state.minSdkLabel)
                } else {
                    stringResource(R.string.app_detail_sdk_version_no_label, version)
                }
                InfoRowItem(
                    label = stringResource(R.string.general_info_min_sdk),
                    value = value,
                    rationale = stringResource(R.string.general_info_rationale_min_sdk),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.targetSdkVersion?.let { version ->
                val value = if (state.targetSdkLabel != null) {
                    stringResource(R.string.app_detail_sdk_version, version, state.targetSdkLabel)
                } else {
                    stringResource(R.string.app_detail_sdk_version_no_label, version)
                }
                InfoRowItem(
                    label = stringResource(R.string.general_info_target_sdk),
                    value = value,
                    rationale = stringResource(R.string.general_info_rationale_target_sdk),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
        }

        InfoSection(title = stringResource(R.string.general_info_section_installation)) {
            InfoRowItem(
                label = stringResource(R.string.general_info_app_type),
                value = if (state.isSystemApp) {
                    stringResource(R.string.general_info_app_type_system)
                } else {
                    stringResource(R.string.general_info_app_type_user)
                },
                rationale = stringResource(R.string.general_info_rationale_app_type),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            InfoRowItem(
                label = stringResource(R.string.general_info_install_source),
                value = state.source,
                rationale = stringResource(R.string.general_info_rationale_install_source),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            state.appInstaller?.let { value ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_installer_package),
                    value = value,
                    rationale = stringResource(R.string.general_info_rationale_installer_package),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.firstInstallTime?.let { instant ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_first_installed),
                    value = formatTimestamp(instant),
                    rationale = stringResource(R.string.general_info_rationale_first_installed),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.lastUpdateTime?.let { instant ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_last_updated),
                    value = formatTimestamp(instant),
                    rationale = stringResource(R.string.general_info_rationale_last_updated),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.lastUsedTime?.let { instant ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_last_used),
                    value = formatTimestamp(instant),
                    rationale = stringResource(R.string.general_info_rationale_last_used),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
        }

        InfoSection(title = stringResource(R.string.general_info_section_storage)) {
            state.apkDirectory?.let { value ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_apk_directory),
                    value = value,
                    rationale = stringResource(R.string.general_info_rationale_apk_directory),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.dataDirectory?.let { value ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_data_directory),
                    value = value,
                    rationale = stringResource(R.string.general_info_rationale_data_directory),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            InfoRowItem(
                label = stringResource(R.string.general_info_install_location),
                value = state.installLocation,
                rationale = stringResource(R.string.general_info_rationale_install_location),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            InfoRowItem(
                label = stringResource(R.string.general_info_apk_size),
                value = state.apkSize.formatted(),
                rationale = stringResource(R.string.general_info_rationale_apk_size),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            state.totalSize?.let { size ->
                InfoRowItem(
                    label = stringResource(R.string.general_info_total_size),
                    value = size.formatted(),
                    rationale = stringResource(R.string.general_info_rationale_total_size),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }

    rationaleRow?.let { row ->
        RationaleBottomSheet(
            row = row,
            onDismiss = { rationaleRow = null },
        )
    }
}

private data class InfoRow(val label: String, val value: String, val rationale: String)

@Composable
private fun InfoSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface),
    ) {
        Text(
            text = title,
            style = AppTheme.typography.titleLarge,
            color = AppTheme.colors.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        content()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun InfoRowItem(
    label: String,
    value: String,
    rationale: String,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .clip(Shapes.CardShape)
            .combinedClickable(
                onClick = { onShowRationale(InfoRow(label, value, rationale)) },
                onLongClick = { onCopy(label, value) },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.onBackground,
        )
    }
}

@Composable
private fun RationaleBottomSheet(
    row: InfoRow,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = row.label,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = row.value,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = row.rationale,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatTimestamp(instant: Instant): String {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return dateFormat.format(Date.from(instant))
}

@Preview
@Composable
private fun GeneralInfoLoadingPreview() {
    ApkAnalyzerTheme {
        GeneralInfoContent(
            state = GeneralInfoState.Loading,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun GeneralInfoErrorPreview() {
    ApkAnalyzerTheme {
        GeneralInfoContent(
            state = GeneralInfoState.Error,
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun GeneralInfoLoadedPreview() {
    ApkAnalyzerTheme {
        GeneralInfoContent(
            state = sampleGeneralInfoState(),
            onAction = {},
            onBack = {},
        )
    }
}

private fun sampleGeneralInfoState() = GeneralInfoState.Loaded(
    applicationName = "Spotify",
    packageName = "com.spotify.music",
    processName = "com.spotify.music",
    uid = 10234,
    description = null,
    versionName = "8.9.42.575",
    versionCode = 120400567,
    minSdkVersion = 24,
    minSdkLabel = "Android 7.0",
    targetSdkVersion = 35,
    targetSdkLabel = "Android 15",
    isSystemApp = false,
    source = "GooglePlay",
    appInstaller = "com.android.vending",
    firstInstallTime = Instant.ofEpochMilli(1_736_640_000_000),
    lastUpdateTime = Instant.ofEpochMilli(1_748_736_000_000),
    lastUsedTime = Instant.ofEpochMilli(1_749_600_000_000),
    apkDirectory = "/data/app/com.spotify.music/base.apk",
    dataDirectory = "/data/data/com.spotify.music",
    installLocation = "Internal",
    apkSize = 152.megabytes,
    totalSize = 510.megabytes,
)
