package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.components.BottomSheet
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes

@Immutable
internal data class InfoRow(val label: String, val value: String, val rationale: String)

@Composable
internal fun InfoRowItem(
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
internal fun RationaleBottomSheet(
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

@Preview
@Composable
private fun InfoRowItemPreview() {
    ApkAnalyzerTheme {
        InfoRowItem(
            label = "Package name",
            value = "com.spotify.music",
            rationale = "The unique identifier of the app on this device.",
            onShowRationale = {},
            onCopy = { _, _ -> },
        )
    }
}
