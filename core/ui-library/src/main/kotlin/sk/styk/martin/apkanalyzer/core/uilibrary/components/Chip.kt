package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppShapes
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun Chip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    trailingIcon: ImageVector? = null,
) {
    val bgColor = if (selected) AppTheme.colors.primaryContainer else AppTheme.colors.surfaceVariant
    val contentColor = if (selected) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(AppShapes.CardShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelLarge,
            color = contentColor,
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor,
            )
        }
    }
}

@Preview
@Composable
private fun ChipUnselectedPreview() {
    ApkAnalyzerTheme {
        Chip(label = "Size", onClick = {})
    }
}

@Preview
@Composable
private fun ChipSelectedPreview() {
    ApkAnalyzerTheme {
        Chip(
            label = "Name",
            onClick = {},
            selected = true,
            trailingIcon = ApkAnalyzerIcons.SortAscending,
        )
    }
}

