package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
internal fun ListSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    explanation: String? = null,
) {
    Column(modifier = modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)) {
        Text(
            text = title,
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.primary,
        )
        if (explanation != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = explanation,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun ListSectionHeaderPreview() {
    ApkAnalyzerTheme {
        ListSectionHeader(title = "ACTIVITIES · 12")
    }
}

@Preview
@Composable
private fun ListSectionHeaderWithExplanationPreview() {
    ApkAnalyzerTheme {
        ListSectionHeader(
            title = "DANGEROUS · 4",
            explanation = "Can access sensitive data or device features. You must grant these explicitly.",
        )
    }
}
