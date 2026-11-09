package sk.styk.martin.apkanalyzer.feature.appdetail.tv.nativelibraries

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryItem
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryVariant
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.kilobytes
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
import sk.styk.martin.apkanalyzer.core.apps.R as AppsR

@Composable
internal fun TvNativeLibrariesScreen(
    appDetailInput: AppDetailInput,
    viewModel: TvNativeLibrariesViewModel = hiltViewModel { factory: TvNativeLibrariesViewModel.Factory ->
        factory.create(appDetailInput)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TvNativeLibrariesContent(state = state, appReference = appDetailInput.iconReference, onAction = viewModel::onAction)
}

@Composable
private fun TvNativeLibrariesContent(
    state: TvNativeLibrariesState,
    appReference: AppReference,
    onAction: (TvNativeLibrariesAction) -> Unit,
) {
    when (state) {
        TvNativeLibrariesState.Loading -> SectionLoading()

        TvNativeLibrariesState.Error -> SectionError(
            message = stringResource(AppsR.string.nativelibraries_error),
            onRetry = { onAction(TvNativeLibrariesAction.Retry) },
        )

        is TvNativeLibrariesState.Loaded -> if (state.items.isEmpty()) {
            SectionEmpty(
                title = stringResource(AppsR.string.nativelibraries_title),
                subtitle = state.appName,
                appReference = appReference,
                message = stringResource(AppsR.string.nativelibraries_empty),
            )
        } else {
            InfoSectionList(
                title = stringResource(AppsR.string.nativelibraries_title),
                subtitle = state.appName,
                appReference = appReference,
                sections = listOf(
                    InfoSection(
                        title = pluralStringResource(AppsR.plurals.nativelibraries_heading, state.items.size, state.items.size),
                        rows = state.items.map { libraryRow(it, state.deviceAbis) },
                    ),
                ),
            )
        }
    }
}

@Composable
private fun libraryRow(item: NativeLibraryItem, deviceAbis: List<String>): InfoRow {
    val support = if (item.isDeviceCompatible) {
        stringResource(AppsR.string.nativelibraries_supported)
    } else {
        stringResource(AppsR.string.nativelibraries_not_supported)
    }
    val variantsLabel = stringResource(AppsR.string.nativelibraries_detail_section_variants)
    val resources = LocalResources.current
    return InfoRow(
        key = item.name,
        label = item.name,
        value = stringResource(R.string.tv_value_pair, item.abis.joinToString(), item.totalSize.formatted()),
        explanation = listOfNotNull(
            support,
            stringResource(AppsR.string.nativelibraries_not_supported_explanation).takeUnless { item.isDeviceCompatible },
            stringResource(AppsR.string.general_info_native_library_device_support, deviceAbis.joinToString()),
            variantsLabel + "\n" + item.variants.joinToString(separator = "\n") { it.describe(resources) },
        ).joinToString(separator = "\n\n"),
        highlighted = !item.isDeviceCompatible,
        icon = ApkAnalyzerIcons.Memory,
        status = support,
    )
}

private fun NativeLibraryVariant.describe(resources: Resources): String = resources.getString(
    R.string.tv_value_pair,
    resources.getString(R.string.tv_value_pair, abi, size.formatted()),
    resources.getString(AppsR.string.nativelibraries_detail_variant_source, containingApkFileName),
)

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TvNativeLibrariesContentPreview() {
    ApkAnalyzerTvTheme {
        TvNativeLibrariesContent(
            appReference = AppReference.InstalledPackage(PackageName("com.google.android.youtube.tv")),
            state = TvNativeLibrariesState.Loaded(
                appName = "YouTube",
                items = listOf(
                    NativeLibraryItem(
                        name = "libcronet.so",
                        abis = listOf("arm64-v8a"),
                        totalSize = 2_400.kilobytes,
                        isDeviceCompatible = true,
                        variants = listOf(NativeLibraryVariant("arm64-v8a", 2_400.kilobytes, "split_config.arm64_v8a.apk")),
                    ),
                ),
                deviceAbis = listOf("arm64-v8a", "armeabi-v7a"),
            ),
            onAction = {},
        )
    }
}
