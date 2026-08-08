package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.ChipVariant
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes

@Suppress("LongParameterList")
@Composable
internal fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    status: String? = null,
    onClickStatus: (() -> Unit)? = null,
    onLongClickStatus: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        ) {
            Text(
                text = title,
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.primary,
                modifier = Modifier.weight(1f),
            )
            if (status != null) {
                Chip(
                    label = status,
                    variant = ChipVariant.Tonal,
                    onClick = onClickStatus ?: {},
                    onLongClick = onLongClickStatus,
                )
            }
        }
        content()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun SectionCardPreview() {
    ApkAnalyzerTheme {
        SectionCard(title = "Identification") {
            Text(
                text = "Content goes here",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun SectionCardWithStatusPreview() {
    ApkAnalyzerTheme {
        SectionCard(
            title = "Signer",
            status = "Self-signed",
            onClickStatus = {},
        ) {
            Text(
                text = "Content goes here",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
