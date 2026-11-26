package sk.styk.martin.apkanalyzer.feature.appdetail.tv.splitapks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.InstalledSplitApk
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.SplitApkKind
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.friendlyQualifier
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.labelRes
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.iconReference
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.R
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoRow
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoSection
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.InfoSectionList
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionEmpty
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionError
import sk.styk.martin.apkanalyzer.feature.appdetail.tv.components.SectionLoading
import sk.styk.martin.apkanalyzer.core.apps.api.R as AppsR
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.R as DomainR

@Composable
internal fun TvSplitApksScreen(
    appDetailInput: AppDetailInput,
    appName: String,
    viewModel: TvSplitApksViewModel = hiltViewModel { factory: TvSplitApksViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TvSplitApksContent(
        state = state,
        appName = appName,
        appReference = appDetailInput.iconReference,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun TvSplitApksContent(
    state: TvSplitApksState,
    appName: String,
    appReference: AppReference,
    onAction: (TvSplitApksAction) -> Unit,
) {
    val title = stringResource(DomainR.string.splitapks_title)
    when (state) {
        TvSplitApksState.Loading -> SectionLoading(
            title = title,
            appName = appName,
            appReference = appReference,
        )

        TvSplitApksState.Error -> SectionError(
            title = title,
            appName = appName,
            appReference = appReference,
            message = stringResource(DomainR.string.splitapks_error),
            onRetry = { onAction(TvSplitApksAction.Retry) },
        )

        is TvSplitApksState.Loaded -> if (state.splits.isEmpty()) {
            SectionEmpty(
                title = title,
                appName = appName,
                appReference = appReference,
                message = stringResource(DomainR.string.splitapks_empty),
            )
        } else {
            InfoSectionList(
                title = title,
                appName = appName,
                appReference = appReference,
                sections = listOf(
                    InfoSection(
                        title = pluralStringResource(DomainR.plurals.splitapks_heading, state.splits.size, state.splits.size),
                        rows = state.splits.map { splitRow(it) },
                    ),
                ),
            )
        }
    }
}

@Composable
private fun splitRow(split: InstalledSplitApk): InfoRow {
    val kind = stringResource(split.kind.labelRes)
    val icon = when (split.kind) {
        SplitApkKind.DynamicFeature -> ApkAnalyzerIcons.Widgets
        SplitApkKind.Abi -> ApkAnalyzerIcons.Memory
        SplitApkKind.ScreenDensity -> ApkAnalyzerIcons.Screen
        SplitApkKind.Language -> ApkAnalyzerIcons.Language
    }
    return InfoRow(
        key = split.fileName,
        label = split.kind.friendlyQualifier(split.qualifier, LocalResources.current),
        value = stringResource(R.string.tv_value_pair, kind, split.size.formatted()),
        explanation = listOfNotNull(
            stringResource(DomainR.string.splitapks_explanation),
            stringResource(R.string.tv_label_value, stringResource(DomainR.string.splitapks_detail_identifier), split.qualifier),
            stringResource(R.string.tv_label_value, stringResource(AppsR.string.splitapks_detail_file_name), split.fileName),
            split.filePath?.let { stringResource(R.string.tv_label_value, stringResource(DomainR.string.splitapks_detail_path), it) },
        ).joinToString(separator = "\n\n"),
        icon = icon,
        status = kind,
    )
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvSplitApksContentPreview() {
    ApkAnalyzerTvTheme {
        TvSplitApksContent(
            appName = "YouTube",
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            state = TvSplitApksState.Loaded(
                splits = listOf(
                    InstalledSplitApk(
                        fileName = "split_config.arm64_v8a.apk",
                        filePath = "/data/app/com.google.android.youtube.tv/split_config.arm64_v8a.apk",
                        size = 12.megabytes,
                        kind = SplitApkKind.Abi,
                        qualifier = "arm64-v8a",
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
