package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes

@Composable
internal fun DetailField(
    label: String,
    value: String,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
    explanation: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .clickable { onCopy(label, value) }
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Text(
            text = value,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onBackground,
        )
        explanation?.let {
            Text(
                text = it,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun DetailFieldPreview() {
    ApkAnalyzerTheme {
        DetailField(
            label = "Full name",
            value = "android.permission.CAMERA",
            onCopy = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun DetailFieldWithExplanationPreview() {
    ApkAnalyzerTheme {
        DetailField(
            label = "On this device",
            value = "Not available",
            explanation = "The app works without it. Whatever depends on it stays off.",
            onCopy = { _, _ -> },
        )
    }
}
