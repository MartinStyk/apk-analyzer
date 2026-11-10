package sk.styk.martin.apkanalyzer.feature.appdetail.tv.generalinfo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.apps.installsource.labelRes
import sk.styk.martin.apkanalyzer.core.apps.installsource.rationaleRes
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.iconReference
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoRow
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoSection
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoSectionList
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionLoading
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.formatTimestamp
import java.time.Instant
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Composable
internal fun TvGeneralInfoScreen(
    appDetailInput: AppDetailInput,
    onNavigateToSplitApks: () -> Unit,
    onNavigateToNativeLibraries: () -> Unit,
    viewModel: TvGeneralInfoViewModel = hiltViewModel { factory: TvGeneralInfoViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TvGeneralInfoContent(
        state = state,
        appReference = appDetailInput.iconReference,
        onNavigateToSplitApks = onNavigateToSplitApks,
        onNavigateToNativeLibraries = onNavigateToNativeLibraries,
    )
}

@Composable
private fun TvGeneralInfoContent(
    state: TvGeneralInfoState,
    appReference: AppReference,
    onNavigateToSplitApks: () -> Unit,
    onNavigateToNativeLibraries: () -> Unit,
) {
    when (state) {
        TvGeneralInfoState.Loading -> SectionLoading()

        TvGeneralInfoState.Error -> SectionError(message = stringResource(AppsR.string.general_info_error))

        is TvGeneralInfoState.Loaded -> InfoSectionList(
            title = stringResource(AppsR.string.general_info_title),
            subtitle = state.applicationName,
            sections = generalInfoSections(state),
            appReference = appReference,
            onClickRow = { row ->
                when (row.key) {
                    SPLIT_APKS_KEY -> onNavigateToSplitApks()
                    NATIVE_LIBRARIES_KEY -> if (state.nativeLibraryCount > 0) onNavigateToNativeLibraries()
                }
            },
            leadsSomewhere = { row -> row.key == SPLIT_APKS_KEY || (row.key == NATIVE_LIBRARIES_KEY && state.nativeLibraryCount > 0) },
        )
    }
}

@Composable
private fun generalInfoSections(state: TvGeneralInfoState.Loaded): List<InfoSection> =
    (identitySections(state) + installationSections(state)).filter { it.rows.isNotEmpty() }

@Composable
private fun identitySections(state: TvGeneralInfoState.Loaded): List<InfoSection> = listOf(
    InfoSection(
        title = stringResource(AppsR.string.general_info_section_identification),
        icon = ApkAnalyzerIcons.Android,
        rows = listOfNotNull(
            row("application-name", AppsR.string.general_info_application_name, state.applicationName, AppsR.string.general_info_rationale_application_name),
            row("package-name", AppsR.string.general_info_package_name, state.packageName.value, AppsR.string.general_info_rationale_package_name),
            state.processName?.let { row("process-name", AppsR.string.general_info_process_name, it, AppsR.string.general_info_rationale_process_name) },
            state.uid?.let { row("uid", AppsR.string.general_info_uid, it.toString(), AppsR.string.general_info_rationale_uid) },
            state.sharedUserId?.let { row("shared-user-id", AppsR.string.general_info_shared_user_id, it, AppsR.string.general_info_rationale_shared_user_id) },
            state.description?.let { row("description", AppsR.string.general_info_description, it, AppsR.string.general_info_rationale_description) },
        ),
    ),
    InfoSection(
        title = stringResource(AppsR.string.general_info_section_versioning),
        icon = ApkAnalyzerIcons.History,
        rows = listOfNotNull(
            state.versionName?.let { row("version-name", AppsR.string.general_info_version_name, it, AppsR.string.general_info_rationale_version_name) },
            row("version-code", AppsR.string.general_info_version_code, state.versionCode.toString(), AppsR.string.general_info_rationale_version_code),
        ),
    ),
    InfoSection(
        title = stringResource(AppsR.string.general_info_section_api_targets),
        icon = ApkAnalyzerIcons.Android,
        rows = listOfNotNull(
            state.minSdkVersion?.let {
                row("min-sdk", AppsR.string.general_info_min_sdk, sdkText(it, state.minSdkLabel), AppsR.string.general_info_rationale_min_sdk)
            },
            state.targetSdkVersion?.let {
                row("target-sdk", AppsR.string.general_info_target_sdk, sdkText(it, state.targetSdkLabel), AppsR.string.general_info_rationale_target_sdk)
            },
        ),
    ),
    InfoSection(
        title = stringResource(AppsR.string.general_info_section_security),
        icon = ApkAnalyzerIcons.Lock,
        rows = listOf(
            row(
                key = "debug-access",
                label = AppsR.string.general_info_debug_access,
                value = stringResource(if (state.isDebuggable) AppsR.string.general_info_enabled else AppsR.string.general_info_disabled),
                explanation = if (state.isDebuggable) {
                    AppsR.string.general_info_rationale_debug_access_enabled
                } else {
                    AppsR.string.general_info_rationale_debug_access_disabled
                },
                highlighted = state.isDebuggable,
            ),
            row(
                key = "backup",
                label = AppsR.string.general_info_app_data_backup,
                value = stringResource(if (state.allowsBackup) AppsR.string.general_info_allowed else AppsR.string.general_info_disabled),
                explanation = if (state.allowsBackup) {
                    AppsR.string.general_info_rationale_backup_allowed
                } else {
                    AppsR.string.general_info_rationale_backup_blocked
                },
            ),
            row(
                key = "cleartext",
                label = AppsR.string.general_info_unencrypted_traffic,
                value = stringResource(if (state.usesCleartextTraffic) AppsR.string.general_info_allowed else AppsR.string.general_info_disabled),
                explanation = if (state.usesCleartextTraffic) {
                    AppsR.string.general_info_rationale_cleartext_allowed
                } else {
                    AppsR.string.general_info_rationale_cleartext_blocked
                },
            ),
        ),
    ),
)

@Composable
private fun installationSections(state: TvGeneralInfoState.Loaded): List<InfoSection> = listOf(
    InfoSection(
        title = stringResource(AppsR.string.general_info_section_installation),
        icon = ApkAnalyzerIcons.FileUpload,
        rows = listOfNotNull(
            row(
                key = "app-type",
                label = AppsR.string.general_info_app_type,
                value = stringResource(if (state.isSystemApp) AppsR.string.general_info_app_type_system else AppsR.string.general_info_app_type_user),
                explanation = AppsR.string.general_info_rationale_app_type,
            ),
            row("install-source", AppsR.string.general_info_install_source, stringResource(state.source.labelRes), state.source.rationaleRes),
            state.installSourceChain.installingPackage?.let {
                row("installer", AppsR.string.general_info_installer_package, it.value, AppsR.string.general_info_rationale_installer_package)
            },
            state.installSourceChain.initiatingPackage?.let {
                row("initiator", AppsR.string.general_info_install_initiator, it.value, AppsR.string.general_info_rationale_install_initiator)
            },
            state.installSourceChain.originatingPackage?.let {
                row("originator", AppsR.string.general_info_install_originator, it.value, AppsR.string.general_info_rationale_install_originator)
            },
            state.firstInstallTime?.let {
                row("first-installed", AppsR.string.general_info_first_installed, formatTimestamp(it), AppsR.string.general_info_rationale_first_installed)
            },
            state.lastUpdateTime?.let {
                row("last-updated", AppsR.string.general_info_last_updated, formatTimestamp(it), AppsR.string.general_info_rationale_last_updated)
            },
            state.lastUsedTime?.let {
                row("last-used", AppsR.string.general_info_last_used, formatTimestamp(it), AppsR.string.general_info_rationale_last_used)
            },
        ),
    ),
    InfoSection(
        title = stringResource(AppsR.string.general_info_section_storage),
        icon = ApkAnalyzerIcons.Storage,
        rows = listOfNotNull(
            state.apkDirectory?.let { row("apk-directory", AppsR.string.general_info_apk_directory, it, AppsR.string.general_info_rationale_apk_directory) },
            state.dataDirectory?.let {
                row("data-directory", AppsR.string.general_info_data_directory, it, AppsR.string.general_info_rationale_data_directory)
            },
            row("install-location", AppsR.string.general_info_install_location, state.installLocation, AppsR.string.general_info_rationale_install_location),
            row("apk-size", AppsR.string.general_info_apk_size, state.apkSize.formatted(), AppsR.string.general_info_rationale_apk_size),
            state.installedSplitsCount.takeIf { it > 0 }?.let { count ->
                InfoRow(
                    key = SPLIT_APKS_KEY,
                    label = stringResource(AppsR.string.general_info_split_apks),
                    value = pluralStringResource(AppsR.plurals.general_info_split_apks_value, count, count),
                    explanation = null,
                )
            },
            state.totalSize?.let { row("total-size", AppsR.string.general_info_total_size, it.formatted(), AppsR.string.general_info_rationale_total_size) },
        ),
    ),
    InfoSection(
        title = stringResource(AppsR.string.general_info_section_native_libraries),
        icon = ApkAnalyzerIcons.Memory,
        rows = listOf(
            InfoRow(
                key = "native-abis",
                label = stringResource(AppsR.string.general_info_native_library_abis),
                value = state.nativeLibraryAbis.takeIf { it.isNotEmpty() }?.joinToString()
                    ?: stringResource(AppsR.string.general_info_native_library_abis_none),
                explanation = nativeAbisExplanation(state),
                highlighted = state.isNativeLibraryDeviceIncompatible,
            ),
            InfoRow(
                key = NATIVE_LIBRARIES_KEY,
                label = stringResource(AppsR.string.general_info_native_library_files),
                value = state.nativeLibraryCount.takeIf { it > 0 }
                    ?.let { pluralStringResource(AppsR.plurals.general_info_native_library_file_count, it, it) }
                    ?: stringResource(AppsR.string.general_info_native_library_files_none),
                explanation = null,
            ),
        ),
    ),
)

@Composable
private fun row(
    key: String,
    label: Int,
    value: String,
    explanation: Int,
    highlighted: Boolean = false,
) = InfoRow(
    key = key,
    label = stringResource(label),
    value = value,
    explanation = stringResource(explanation),
    highlighted = highlighted,
)

@Composable
private fun sdkText(version: Int, label: String?): String = if (label != null) {
    stringResource(AppsR.string.app_detail_sdk_version, version, label)
} else {
    stringResource(AppsR.string.app_detail_sdk_version_no_label, version)
}

@Composable
private fun nativeAbisExplanation(state: TvGeneralInfoState.Loaded): String = listOfNotNull(
    stringResource(AppsR.string.general_info_rationale_native_library_abis),
    stringResource(AppsR.string.general_info_native_library_device_support, state.deviceSupportedAbis.joinToString()),
    stringResource(AppsR.string.general_info_native_library_incompatible).takeIf { state.isNativeLibraryDeviceIncompatible },
).joinToString(separator = " ")

private const val SPLIT_APKS_KEY = "split-apks"
private const val NATIVE_LIBRARIES_KEY = "native-libraries"

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvGeneralInfoContentPreview() {
    ApkAnalyzerTvTheme {
        TvGeneralInfoContent(
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            state = TvGeneralInfoState.Loaded(
                applicationName = "YouTube",
                packageName = PackageName("com.google.android.youtube.tv"),
                processName = "com.google.android.youtube.tv",
                uid = 10_123,
                sharedUserId = null,
                description = null,
                versionName = "4.40.303",
                versionCode = 1_440_303,
                minSdkVersion = 26,
                minSdkLabel = "Android 8.0",
                targetSdkVersion = 34,
                targetSdkLabel = "Android 14",
                isSystemApp = false,
                isDebuggable = false,
                allowsBackup = true,
                usesCleartextTraffic = false,
                source = AppSource.GooglePlay,
                installSourceChain = InstallSourceChain(installingPackage = PackageName("com.android.vending")),
                firstInstallTime = Instant.parse("2025-03-02T09:00:00Z"),
                lastUpdateTime = Instant.parse("2026-09-28T10:15:30Z"),
                lastUsedTime = null,
                apkDirectory = "/data/app/com.google.android.youtube.tv/base.apk",
                dataDirectory = "/example/user/0/com.google.android.youtube.tv",
                installLocation = "AUTO",
                apkSize = 92.megabytes,
                totalSize = 184.megabytes,
                nativeLibraryAbis = listOf("arm64-v8a"),
                nativeLibraryCount = 12,
                deviceSupportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
                isNativeLibraryDeviceIncompatible = false,
                installedSplitsCount = 3,
            ),
            onNavigateToSplitApks = {},
            onNavigateToNativeLibraries = {},
        )
    }
}
