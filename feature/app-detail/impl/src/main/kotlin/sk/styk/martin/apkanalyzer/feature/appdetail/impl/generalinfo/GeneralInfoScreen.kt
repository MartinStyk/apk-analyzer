package sk.styk.martin.apkanalyzer.feature.appdetail.impl.generalinfo

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.uilibrary.components.LoadingSpinner
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Toolbar
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.InfoRow
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.InfoRowItem
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.RationaleBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionCard
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.SectionLoading
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
            is GeneralInfoState.Loading -> SectionLoading()
            is GeneralInfoState.Error -> SectionError(message = stringResource(R.string.general_info_error))
            is GeneralInfoState.Loaded -> LoadedContent(state = state, onAction = onAction)
        }
    }
}

@Suppress("LongMethod")
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = stringResource(R.string.general_info_section_identification)) {
            InfoRowItem(
                label = stringResource(R.string.general_info_application_name),
                value = state.applicationName,
                rationale = stringResource(R.string.general_info_rationale_application_name),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            InfoRowItem(
                label = stringResource(R.string.general_info_package_name),
                value = state.packageName.value,
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

        SectionCard(title = stringResource(R.string.general_info_section_versioning)) {
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

        SectionCard(title = stringResource(R.string.general_info_section_api_targets)) {
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

        SecuritySettingsSection(
            state = state,
            onShowRationale = { rationaleRow = it },
            onCopy = onCopy,
        )

        SectionCard(title = stringResource(R.string.general_info_section_installation)) {
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
                    value = value.value,
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

        SectionCard(title = stringResource(R.string.general_info_section_storage)) {
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

@Suppress("LongMethod")
@Composable
private fun SecuritySettingsSection(
    state: GeneralInfoState.Loaded,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(R.string.general_info_section_security),
        modifier = modifier,
    ) {
        InfoRowItem(
            label = stringResource(R.string.general_info_debug_access),
            value = stringResource(
                if (state.isDebuggable) {
                    R.string.general_info_enabled
                } else {
                    R.string.general_info_disabled
                },
            ),
            rationale = stringResource(
                if (state.isDebuggable) {
                    R.string.general_info_rationale_debug_access_enabled
                } else {
                    R.string.general_info_rationale_debug_access_disabled
                },
            ),
            onShowRationale = onShowRationale,
            onCopy = onCopy,
        )
        InfoRowItem(
            label = stringResource(R.string.general_info_app_data_backup),
            value = stringResource(
                if (state.allowsBackup) {
                    R.string.general_info_allowed
                } else {
                    R.string.general_info_disabled
                },
            ),
            rationale = stringResource(
                if (state.allowsBackup) {
                    R.string.general_info_rationale_backup_allowed
                } else {
                    R.string.general_info_rationale_backup_blocked
                },
            ),
            onShowRationale = onShowRationale,
            onCopy = onCopy,
        )
        InfoRowItem(
            label = stringResource(R.string.general_info_unencrypted_traffic),
            value = stringResource(
                if (state.usesCleartextTraffic) {
                    R.string.general_info_allowed
                } else {
                    R.string.general_info_disabled
                },
            ),
            rationale = stringResource(
                if (state.usesCleartextTraffic) {
                    R.string.general_info_rationale_cleartext_allowed
                } else {
                    R.string.general_info_rationale_cleartext_blocked
                },
            ),
            onShowRationale = onShowRationale,
            onCopy = onCopy,
        )
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
    packageName = PackageName("com.spotify.music"),
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
    isDebuggable = true,
    allowsBackup = false,
    usesCleartextTraffic = true,
    source = "GooglePlay",
    appInstaller = PackageName("com.android.vending"),
    firstInstallTime = Instant.ofEpochMilli(1_736_640_000_000),
    lastUpdateTime = Instant.ofEpochMilli(1_748_736_000_000),
    lastUsedTime = Instant.ofEpochMilli(1_749_600_000_000),
    apkDirectory = "/data/app/com.spotify.music/base.apk",
    dataDirectory = "/data/data/com.spotify.music",
    installLocation = "Internal",
    apkSize = 152.megabytes,
    totalSize = 510.megabytes,
)
