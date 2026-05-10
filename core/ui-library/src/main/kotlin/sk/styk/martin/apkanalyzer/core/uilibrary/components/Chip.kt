package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    leadingIcon: ImageVector? = null,
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
        trailingIcon = trailingIcon?.let {
            {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
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

@Composable
fun OutlinedChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    trailingIcon: ImageVector? = null,
    leadingIcon: ImageVector? = null,
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
        trailingIcon = trailingIcon?.let {
            {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        shape = Shapes.CardShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = AppTheme.colors.onSurfaceVariant,
            iconColor = AppTheme.colors.onSurfaceVariant,
            selectedContainerColor = AppTheme.colors.secondaryContainer,
            selectedLabelColor = AppTheme.colors.onSecondaryContainer,
            selectedLeadingIconColor = AppTheme.colors.onSecondaryContainer,
            selectedTrailingIconColor = AppTheme.colors.onSecondaryContainer,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) AppTheme.colors.onSecondaryContainer else AppTheme.colors.onSurfaceVariant,
        ),
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

@Preview
@Composable
private fun OutlinedChipUnselectedPreview() {
    ApkAnalyzerTheme {
        OutlinedChip(
            label = "Match: Any",
            onClick = {},
            trailingIcon = ApkAnalyzerIcons.ArrowDropDown,
        )
    }
}

@Preview
@Composable
private fun OutlinedChipSelectedPreview() {
    ApkAnalyzerTheme {
        OutlinedChip(
            label = "Match: All",
            onClick = {},
            selected = true,
            trailingIcon = ApkAnalyzerIcons.ArrowDropDown,
        )
    }
}
