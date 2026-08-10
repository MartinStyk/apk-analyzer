package sk.styk.martin.apkanalyzer.feature.appdetail.impl.splitapks

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
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledSplitApk
import sk.styk.martin.apkanalyzer.core.apps.model.SplitApkKind
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.components.DetailField

@Composable
internal fun SplitApkDetailBottomSheet(
    split: InstalledSplitApk,
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
                    imageVector = split.kind.icon,
                    contentDescription = stringResource(split.kind.labelRes),
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = split.kind.friendlyQualifier(split.qualifier),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            DetailField(
                label = stringResource(R.string.splitapks_detail_type),
                value = stringResource(split.kind.labelRes),
                onCopy = onCopy,
            )
            DetailField(
                label = stringResource(R.string.splitapks_detail_identifier),
                value = split.qualifier,
                onCopy = onCopy,
            )
            DetailField(
                label = stringResource(R.string.splitapks_detail_file_name),
                value = split.fileName,
                onCopy = onCopy,
            )
            DetailField(
                label = stringResource(R.string.splitapks_detail_size),
                value = split.size.formatted(),
                onCopy = onCopy,
            )
            DetailField(
                label = stringResource(R.string.splitapks_detail_path),
                value = split.filePath,
                onCopy = onCopy,
            )
        }
    }
}

@Preview
@Composable
private fun SplitApkDetailBottomSheetPreview() {
    ApkAnalyzerTheme {
        SplitApkDetailBottomSheet(
            split = InstalledSplitApk(
                fileName = "split_config.arm64_v8a.apk",
                filePath = "/data/app/com.spotify.music/split_config.arm64_v8a.apk",
                size = 24.megabytes,
                kind = SplitApkKind.Abi,
                qualifier = "arm64-v8a",
            ),
            onCopy = { _, _ -> },
            onDismiss = {},
        )
    }
}
