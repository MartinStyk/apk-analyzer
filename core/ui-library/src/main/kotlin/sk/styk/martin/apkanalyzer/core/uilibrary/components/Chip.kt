package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes

@Composable
fun Chip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    trailingIcon: ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = AppTheme.typography.labelLarge,
            )
        },
        modifier = modifier,
        trailingIcon = if (trailingIcon != null) {
            {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
        shape = Shapes.CardShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = AppTheme.colors.surfaceVariant,
            labelColor = AppTheme.colors.onSurfaceVariant,
            iconColor = AppTheme.colors.onSurfaceVariant,
        ),
        border = null,
    )
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
