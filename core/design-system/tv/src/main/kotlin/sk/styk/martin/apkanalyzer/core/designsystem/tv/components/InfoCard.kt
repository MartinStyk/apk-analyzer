package sk.styk.martin.apkanalyzer.core.designsystem.tv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.pointerClickable
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import androidx.tv.material3.Card as TvMaterialCard

@Composable
fun InfoCard(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: InfoCardTone = InfoCardTone.Default,
    stackedHeader: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val accent = when (tone) {
        InfoCardTone.Default -> AppTheme.colors.primary
        InfoCardTone.Warning -> AppTheme.colors.warning
    }
    val container = when (tone) {
        InfoCardTone.Default -> AppTheme.colors.surface
        InfoCardTone.Warning -> AppTheme.colors.warningContainer
    }
    TvMaterialCard(
        onClick = onClick,
        modifier = modifier.pointerClickable(CardShape, onClick),
        shape = CardDefaults.shape(shape = CardShape),
        colors = CardDefaults.colors(
            containerColor = container,
            contentColor = AppTheme.colors.onSurface,
            focusedContainerColor = if (tone == InfoCardTone.Warning) container else AppTheme.colors.surfaceVariant,
            focusedContentColor = AppTheme.colors.onSurface,
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = FocusBorderWidth, color = accent),
                shape = CardShape,
            ),
        ),
        scale = CardDefaults.scale(focusedScale = 1.04f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (stackedHeader) {
                if (icon != null) {
                    IconBadge(icon = icon, tint = accent, size = 28.dp)
                }
                CardTitle(title = title, maxLines = 1)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (icon != null) {
                        IconBadge(icon = icon, tint = accent, size = 28.dp)
                    }
                    CardTitle(title = title, maxLines = 3)
                }
            }
            content()
        }
    }
}

@Composable
private fun CardTitle(title: String, maxLines: Int) {
    Text(text = title, style = AppTheme.typography.titleMedium, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

private val CardShape = RoundedCornerShape(12.dp)

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun InfoCardPreview() {
    ApkAnalyzerTvTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(48.dp)) {
            InfoCard(onClick = {}, title = "General info", icon = ApkAnalyzerIcons.Info, modifier = Modifier.width(360.dp)) {
                Text(text = "Version 4.40.303", color = AppTheme.colors.onSurfaceVariant)
                Text(text = "128 MB", color = AppTheme.colors.onSurfaceVariant)
            }
            InfoCard(
                onClick = {},
                title = "Can read your contacts",
                icon = ApkAnalyzerIcons.Warning,
                tone = InfoCardTone.Warning,
                modifier = Modifier.width(360.dp),
            )
        }
    }
}
