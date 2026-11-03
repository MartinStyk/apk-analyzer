package sk.styk.martin.apkanalyzer.feature.appdetail.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R

@Composable
internal fun AppSummaryBottomSheet(
    summary: String,
    canExportInventory: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onExportInventory: () -> Unit,
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
            Text(
                text = stringResource(R.string.app_detail_summary_sheet_title),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(Shapes.CardShape)
                    .background(AppTheme.colors.surfaceVariant)
                    .clickable(onClick = onCopy)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    text = summary,
                    style = AppTheme.typography.monospace,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    text = stringResource(R.string.app_detail_action_copy_summary),
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                )
                if (canExportInventory) {
                    Button(
                        text = stringResource(R.string.app_detail_action_export_inventory),
                        onClick = onExportInventory,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Button(
                        text = stringResource(R.string.app_detail_action_share_summary),
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppSummaryBottomSheetPreview() {
    ApkAnalyzerTheme {
        AppSummaryBottomSheet(
            summary = "Spotify (com.spotify.music)\n" +
                "Version: 9.4.12 (90412)\n" +
                "APK Size: 152 MB\n" +
                "Permissions: 32 requested permissions · 6 dangerous permissions · 4 granted permissions",
            canExportInventory = true,
            onCopy = {},
            onShare = {},
            onExportInventory = {},
            onDismiss = {},
        )
    }
}
