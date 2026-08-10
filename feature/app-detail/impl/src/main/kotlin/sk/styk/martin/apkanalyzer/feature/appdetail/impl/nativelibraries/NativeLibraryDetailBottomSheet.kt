package sk.styk.martin.apkanalyzer.feature.appdetail.impl.nativelibraries

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.common.model.kilobytes
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.DetailField

@Composable
internal fun NativeLibraryDetailBottomSheet(
    item: NativeLibraryItem,
    onCopy: (label: String, value: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ApkAnalyzerIcons.Memory,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.name,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            DetailField(
                label = stringResource(R.string.nativelibraries_detail_device_support),
                value = stringResource(
                    if (item.isDeviceCompatible) R.string.nativelibraries_supported else R.string.nativelibraries_not_supported,
                ),
                explanation = if (item.isDeviceCompatible) {
                    null
                } else {
                    stringResource(R.string.nativelibraries_not_supported_explanation)
                },
                onCopy = onCopy,
            )
            DetailField(
                label = stringResource(R.string.nativelibraries_detail_abis),
                value = item.abis.joinToString(),
                onCopy = onCopy,
            )
            DetailField(
                label = stringResource(R.string.nativelibraries_detail_total_size),
                value = item.totalSize.formatted(),
                onCopy = onCopy,
            )

            Column(modifier = Modifier.padding(top = 20.dp)) {
                Text(
                    text = stringResource(R.string.nativelibraries_detail_section_variants),
                    style = AppTheme.typography.titleSmall,
                    color = AppTheme.colors.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                item.variants.forEachIndexed { index, variant ->
                    if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                    DetailField(
                        label = variant.abi,
                        value = variant.size.formatted(),
                        explanation = stringResource(R.string.nativelibraries_detail_variant_source, variant.containingApkFileName),
                        onCopy = onCopy,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NativeLibraryDetailBottomSheetPreview() {
    ApkAnalyzerTheme {
        NativeLibraryDetailBottomSheet(
            item = NativeLibraryItem(
                name = "libcrashlytics.so",
                abis = persistentListOf("arm64-v8a", "armeabi-v7a"),
                totalSize = 640.kilobytes,
                isDeviceCompatible = true,
                variants = persistentListOf(
                    NativeLibraryVariant("arm64-v8a", 340.kilobytes, "base.apk"),
                    NativeLibraryVariant("armeabi-v7a", 300.kilobytes, "base.apk"),
                ),
            ),
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun NativeLibraryDetailBottomSheetIncompatiblePreview() {
    ApkAnalyzerTheme {
        NativeLibraryDetailBottomSheet(
            item = NativeLibraryItem(
                name = "libx86only.so",
                abis = persistentListOf("x86"),
                totalSize = 640.kilobytes,
                isDeviceCompatible = false,
                variants = persistentListOf(
                    NativeLibraryVariant("x86", 640.kilobytes, "split_config.x86.apk"),
                ),
            ),
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}
