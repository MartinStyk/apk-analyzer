package sk.styk.martin.apkanalyzer.feature.appdetail.phone.generalinfo

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.apps.installsource.labelRes
import sk.styk.martin.apkanalyzer.core.apps.installsource.rationaleRes
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.InfoRow
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.InfoRowItem
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.NavigableInfoRowItem
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.RationaleBottomSheet
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.RecordedVersionError
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionCard
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.SectionLoading
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import sk.styk.martin.apkanalyzer.core.apphistory.R as HistoryR
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Composable
internal fun GeneralInfoScreen(
    appDetailInput: AppDetailInput,
    onBack: () -> Unit,
    onNavigateToSplitApks: () -> Unit,
    onNavigateToNativeLibraries: () -> Unit,
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
        onNavigateToSplitApks = onNavigateToSplitApks,
        onNavigateToNativeLibraries = onNavigateToNativeLibraries,
        modifier = modifier,
    )
}

@Composable
private fun GeneralInfoContent(
    state: GeneralInfoState,
    onAction: (GeneralInfoAction) -> Unit,
    onBack: () -> Unit,
    onNavigateToSplitApks: () -> Unit,
    onNavigateToNativeLibraries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(AppsR.string.general_info_title),
            onBack = onBack,
        )
        when (state) {
            is GeneralInfoState.Loading -> SectionLoading()

            is GeneralInfoState.RecordedVersionError -> RecordedVersionError(
                failure = state.failure,
                onRetry = { onAction(GeneralInfoAction.Retry) },
            )

            is GeneralInfoState.Error -> SectionError(message = stringResource(AppsR.string.general_info_error))

            is GeneralInfoState.Loaded -> LoadedContent(
                state = state,
                onAction = onAction,
                onNavigateToSplitApks = onNavigateToSplitApks,
                onNavigateToNativeLibraries = onNavigateToNativeLibraries,
            )
        }
    }
}

@Composable
private fun LoadedContent(
    state: GeneralInfoState.Loaded,
    onAction: (GeneralInfoAction) -> Unit,
    onNavigateToSplitApks: () -> Unit,
    onNavigateToNativeLibraries: () -> Unit,
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
        SectionCard(title = stringResource(AppsR.string.general_info_section_identification)) {
            InfoRowItem(
                label = stringResource(AppsR.string.general_info_application_name),
                value = state.applicationName,
                rationale = stringResource(AppsR.string.general_info_rationale_application_name),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            InfoRowItem(
                label = stringResource(AppsR.string.general_info_package_name),
                value = state.packageName.value,
                rationale = stringResource(AppsR.string.general_info_rationale_package_name),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            state.processName?.let { value ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_process_name),
                    value = value,
                    rationale = stringResource(AppsR.string.general_info_rationale_process_name),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.uid?.let { value ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_uid),
                    value = value.toString(),
                    rationale = stringResource(AppsR.string.general_info_rationale_uid),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.sharedUserId?.let { value ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_shared_user_id),
                    value = value,
                    rationale = stringResource(AppsR.string.general_info_rationale_shared_user_id),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.description?.let { value ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_description),
                    value = value,
                    rationale = stringResource(AppsR.string.general_info_rationale_description),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
        }

        SectionCard(title = stringResource(AppsR.string.general_info_section_versioning)) {
            state.versionName?.let { value ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_version_name),
                    value = value,
                    rationale = stringResource(AppsR.string.general_info_rationale_version_name),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            InfoRowItem(
                label = stringResource(AppsR.string.general_info_version_code),
                value = state.versionCode.toString(),
                rationale = stringResource(AppsR.string.general_info_rationale_version_code),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
        }

        SectionCard(title = stringResource(AppsR.string.general_info_section_api_targets)) {
            state.minSdkVersion?.let { version ->
                val value = if (state.minSdkLabel != null) {
                    stringResource(AppsR.string.app_detail_sdk_version, version, state.minSdkLabel)
                } else {
                    stringResource(AppsR.string.app_detail_sdk_version_no_label, version)
                }
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_min_sdk),
                    value = value,
                    rationale = stringResource(AppsR.string.general_info_rationale_min_sdk),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.targetSdkVersion?.let { version ->
                val value = if (state.targetSdkLabel != null) {
                    stringResource(AppsR.string.app_detail_sdk_version, version, state.targetSdkLabel)
                } else {
                    stringResource(AppsR.string.app_detail_sdk_version_no_label, version)
                }
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_target_sdk),
                    value = value,
                    rationale = stringResource(AppsR.string.general_info_rationale_target_sdk),
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

        SectionCard(title = stringResource(AppsR.string.general_info_section_installation)) {
            InfoRowItem(
                label = stringResource(AppsR.string.general_info_app_type),
                value = if (state.isSystemApp) {
                    stringResource(AppsR.string.general_info_app_type_system)
                } else {
                    stringResource(AppsR.string.general_info_app_type_user)
                },
                rationale = stringResource(AppsR.string.general_info_rationale_app_type),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            InfoRowItem(
                label = stringResource(AppsR.string.general_info_install_source),
                value = stringResource(state.source.labelRes),
                rationale = stringResource(state.source.rationaleRes),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            state.installSourceChain.installingPackage?.let { value ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_installer_package),
                    value = value.value,
                    rationale = stringResource(AppsR.string.general_info_rationale_installer_package),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.installSourceChain.initiatingPackage?.let { value ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_install_initiator),
                    value = value.value,
                    rationale = stringResource(AppsR.string.general_info_rationale_install_initiator),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.installSourceChain.originatingPackage?.let { value ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_install_originator),
                    value = value.value,
                    rationale = stringResource(AppsR.string.general_info_rationale_install_originator),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.firstInstallTime?.let { instant ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_first_installed),
                    value = formatTimestamp(instant),
                    rationale = stringResource(AppsR.string.general_info_rationale_first_installed),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.lastUpdateTime?.let { instant ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_last_updated),
                    value = formatTimestamp(instant),
                    rationale = stringResource(AppsR.string.general_info_rationale_last_updated),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.lastUsedTime?.let { instant ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_last_used),
                    value = formatTimestamp(instant),
                    rationale = stringResource(AppsR.string.general_info_rationale_last_used),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
        }

        SectionCard(title = stringResource(AppsR.string.general_info_section_storage)) {
            state.apkDirectory?.let { value ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_apk_directory),
                    value = value,
                    rationale = stringResource(AppsR.string.general_info_rationale_apk_directory),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            state.dataDirectory?.let { value ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_data_directory),
                    value = value,
                    rationale = stringResource(AppsR.string.general_info_rationale_data_directory),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
            InfoRowItem(
                label = stringResource(AppsR.string.general_info_install_location),
                value = state.installLocation,
                rationale = stringResource(AppsR.string.general_info_rationale_install_location),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            InfoRowItem(
                label = stringResource(AppsR.string.general_info_apk_size),
                value = state.apkSize.formatted(),
                rationale = stringResource(AppsR.string.general_info_rationale_apk_size),
                onShowRationale = { rationaleRow = it },
                onCopy = onCopy,
            )
            if (state.installedSplitsCount > 0) {
                NavigableInfoRowItem(
                    label = stringResource(AppsR.string.general_info_split_apks),
                    value = pluralStringResource(
                        AppsR.plurals.general_info_split_apks_value,
                        state.installedSplitsCount,
                        state.installedSplitsCount,
                    ),
                    onClick = onNavigateToSplitApks,
                    onCopy = onCopy,
                )
            }
            state.totalSize?.let { size ->
                InfoRowItem(
                    label = stringResource(AppsR.string.general_info_total_size),
                    value = size.formatted(),
                    rationale = stringResource(AppsR.string.general_info_rationale_total_size),
                    onShowRationale = { rationaleRow = it },
                    onCopy = onCopy,
                )
            }
        }

        NativeLibrariesSection(
            state = state,
            onShowRationale = { rationaleRow = it },
            onCopy = onCopy,
            onNavigateToNativeLibraries = onNavigateToNativeLibraries,
        )

        Spacer(modifier = Modifier.height(4.dp))
    }

    rationaleRow?.let { row ->
        RationaleBottomSheet(
            row = row,
            onDismiss = { rationaleRow = null },
        )
    }
}

@Composable
private fun SecuritySettingsSection(
    state: GeneralInfoState.Loaded,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(AppsR.string.general_info_section_security),
        modifier = modifier,
    ) {
        InfoRowItem(
            label = stringResource(AppsR.string.general_info_debug_access),
            value = stringResource(
                if (state.isDebuggable) {
                    AppsR.string.general_info_enabled
                } else {
                    AppsR.string.general_info_disabled
                },
            ),
            rationale = stringResource(
                if (state.isDebuggable) {
                    AppsR.string.general_info_rationale_debug_access_enabled
                } else {
                    AppsR.string.general_info_rationale_debug_access_disabled
                },
            ),
            onShowRationale = onShowRationale,
            onCopy = onCopy,
        )
        InfoRowItem(
            label = stringResource(AppsR.string.general_info_app_data_backup),
            value = stringResource(
                if (state.allowsBackup) {
                    AppsR.string.general_info_allowed
                } else {
                    AppsR.string.general_info_disabled
                },
            ),
            rationale = stringResource(
                if (state.allowsBackup) {
                    AppsR.string.general_info_rationale_backup_allowed
                } else {
                    AppsR.string.general_info_rationale_backup_blocked
                },
            ),
            onShowRationale = onShowRationale,
            onCopy = onCopy,
        )
        InfoRowItem(
            label = stringResource(AppsR.string.general_info_unencrypted_traffic),
            value = stringResource(
                if (state.usesCleartextTraffic) {
                    AppsR.string.general_info_allowed
                } else {
                    AppsR.string.general_info_disabled
                },
            ),
            rationale = stringResource(
                if (state.usesCleartextTraffic) {
                    AppsR.string.general_info_rationale_cleartext_allowed
                } else {
                    AppsR.string.general_info_rationale_cleartext_blocked
                },
            ),
            onShowRationale = onShowRationale,
            onCopy = onCopy,
        )
    }
}

@Composable
private fun NativeLibrariesSection(
    state: GeneralInfoState.Loaded,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    onNavigateToNativeLibraries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(AppsR.string.general_info_section_native_libraries),
        modifier = modifier,
    ) {
        if (!state.isNativeLibrariesRecorded) {
            InfoRowItem(
                label = stringResource(AppsR.string.general_info_native_library_abis),
                value = stringResource(HistoryR.string.app_history_not_recorded),
                rationale = stringResource(HistoryR.string.app_history_not_recorded_explanation),
                onShowRationale = onShowRationale,
                onCopy = onCopy,
            )
            return@SectionCard
        }
        val abisValue = if (state.nativeLibraryAbis.isNotEmpty()) {
            state.nativeLibraryAbis.joinToString()
        } else {
            stringResource(AppsR.string.general_info_native_library_abis_none)
        }
        val abisRationale = buildString {
            append(stringResource(AppsR.string.general_info_rationale_native_library_abis))
            if (state.deviceSupportedAbis.isNotEmpty()) {
                append(' ')
                append(
                    stringResource(
                        AppsR.string.general_info_native_library_device_support,
                        state.deviceSupportedAbis.joinToString(),
                    ),
                )
            }
            if (state.isNativeLibraryDeviceIncompatible) {
                append(' ')
                append(stringResource(AppsR.string.general_info_native_library_incompatible))
            }
        }
        InfoRowItem(
            label = stringResource(AppsR.string.general_info_native_library_abis),
            value = abisValue,
            rationale = abisRationale,
            onShowRationale = onShowRationale,
            onCopy = onCopy,
        )

        val fileCount = state.nativeLibraryNames.size
        val filesValue = if (fileCount > 0) {
            pluralStringResource(AppsR.plurals.general_info_native_library_file_count, fileCount, fileCount)
        } else {
            stringResource(AppsR.string.general_info_native_library_files_none)
        }
        NavigableInfoRowItem(
            label = stringResource(AppsR.string.general_info_native_library_files),
            value = filesValue,
            onClick = onNavigateToNativeLibraries,
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
            onNavigateToSplitApks = {},
            onNavigateToNativeLibraries = {},
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
            onNavigateToSplitApks = {},
            onNavigateToNativeLibraries = {},
        )
    }
}

@Preview(heightDp = 2400)
@Composable
private fun GeneralInfoNotRecordedPreview() {
    ApkAnalyzerTheme {
        GeneralInfoContent(
            state = sampleGeneralInfoState().copy(
                isNativeLibrariesRecorded = false,
                nativeLibraryAbis = emptyList(),
                nativeLibraryNames = emptyList(),
                deviceSupportedAbis = emptyList(),
                totalSize = null,
                lastUsedTime = null,
                apkDirectory = null,
                dataDirectory = null,
            ),
            onAction = {},
            onBack = {},
            onNavigateToSplitApks = {},
            onNavigateToNativeLibraries = {},
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
            onNavigateToSplitApks = {},
            onNavigateToNativeLibraries = {},
        )
    }
}

private fun sampleGeneralInfoState() = GeneralInfoState.Loaded(
    applicationName = "Spotify",
    packageName = PackageName("com.spotify.music"),
    processName = "com.spotify.music",
    uid = 10234,
    sharedUserId = null,
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
    source = AppSource.GooglePlay,
    installSourceChain = InstallSourceChain(
        installingPackage = PackageName("com.android.vending"),
        initiatingPackage = PackageName("com.android.vending"),
        originatingPackage = null,
    ),
    firstInstallTime = Instant.ofEpochMilli(1_736_640_000_000),
    lastUpdateTime = Instant.ofEpochMilli(1_748_736_000_000),
    lastUsedTime = Instant.ofEpochMilli(1_749_600_000_000),
    apkDirectory = "/example/app/com.spotify.music/base.apk",
    dataDirectory = "/example/user/0/com.spotify.music",
    installLocation = "Internal",
    apkSize = 152.megabytes,
    totalSize = 510.megabytes,
    nativeLibraryAbis = listOf("arm64-v8a", "armeabi-v7a"),
    nativeLibraryNames = listOf("libapp.so", "libcrashlytics.so", "libcrypto.so"),
    deviceSupportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
    isNativeLibraryDeviceIncompatible = false,
    installedSplitsCount = 3,
)
