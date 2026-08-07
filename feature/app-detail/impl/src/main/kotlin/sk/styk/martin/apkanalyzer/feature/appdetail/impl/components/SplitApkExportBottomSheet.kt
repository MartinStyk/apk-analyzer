package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Button
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R

@Composable
internal fun SplitApkExportBottomSheet(
    displayName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = ApkAnalyzerIcons.Warning,
                    tint = AppTheme.colors.warning,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.app_detail_split_apk_saved_title),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.app_detail_split_apk_saved_description, displayName),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
            Button(
                text = stringResource(R.string.app_detail_split_apk_saved_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun SplitApkExportBottomSheetPreview() {
    ApkAnalyzerTheme {
        SplitApkExportBottomSheet(
            displayName = "com.instagram.android.apk",
            onDismiss = {},
        )
    }
}
