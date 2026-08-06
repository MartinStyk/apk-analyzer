package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes

@Immutable
sealed interface ChipVariant {
    data object Default : ChipVariant
    data object Tonal : ChipVariant
    data object Positive : ChipVariant
    data object Warning : ChipVariant
    data object Negative : ChipVariant
}

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
            selectedContainerColor = AppTheme.colors.secondaryContainer,
            selectedLabelColor = AppTheme.colors.onSecondaryContainer,
            selectedLeadingIconColor = AppTheme.colors.onSecondaryContainer,
            selectedTrailingIconColor = AppTheme.colors.onSecondaryContainer,
        ),
        border = null,
    )
}

@Composable
fun Chip(
    label: String,
    variant: ChipVariant,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val (containerColor, contentColor) = variant.colors()
    Surface(
        modifier = modifier,
        shape = Shapes.CardShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        ChipContent(
            label = label,
            leadingIcon = leadingIcon,
            contentColor = contentColor,
        )
    }
}

@Composable
fun Chip(
    label: String,
    variant: ChipVariant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
) {
    val (containerColor, contentColor) = variant.colors()
    if (onLongClick == null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = Shapes.CardShape,
            color = containerColor,
            contentColor = contentColor,
        ) {
            ChipContent(
                label = label,
                leadingIcon = leadingIcon,
                contentColor = contentColor,
            )
        }
    } else {
        Surface(
            modifier = modifier
                .clip(Shapes.CardShape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = Shapes.CardShape,
            color = containerColor,
            contentColor = contentColor,
        ) {
            ChipContent(
                label = label,
                leadingIcon = leadingIcon,
                contentColor = contentColor,
            )
        }
    }
}

@Composable
private fun ChipContent(
    label: String,
    leadingIcon: ImageVector?,
    contentColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            style = AppTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun ChipVariant.colors() = when (this) {
    ChipVariant.Default -> AppTheme.colors.surfaceVariant to AppTheme.colors.onSurfaceVariant
    ChipVariant.Tonal -> AppTheme.colors.secondaryContainer to AppTheme.colors.onSecondaryContainer
    ChipVariant.Positive -> AppTheme.colors.positiveContainer to AppTheme.colors.positive
    ChipVariant.Warning -> AppTheme.colors.warningContainer to AppTheme.colors.warning
    ChipVariant.Negative -> AppTheme.colors.negativeContainer to AppTheme.colors.negative
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
private fun ChipSurfacePreview() {
    ApkAnalyzerTheme {
        Chip(label = "Size", variant = ChipVariant.Default)
    }
}

@Preview
@Composable
private fun ChipTonalPreview() {
    ApkAnalyzerTheme {
        Chip(label = "v2.1.0 (210)", variant = ChipVariant.Tonal)
    }
}

@Preview
@Composable
private fun ChipWarningPreview() {
    ApkAnalyzerTheme {
        Chip(label = "SDK 28", variant = ChipVariant.Warning)
    }
}

@Preview
@Composable
private fun ChipPositivePreview() {
    ApkAnalyzerTheme {
        Chip(
            label = "Valid",
            variant = ChipVariant.Positive,
            onClick = {},
            onLongClick = {},
        )
    }
}

@Preview
@Composable
private fun ChipNegativeDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        Chip(label = "Not valid yet", variant = ChipVariant.Negative)
    }
}

@Preview
@Composable
private fun ChipClickableUnselectedPreview() {
    ApkAnalyzerTheme {
        Chip(label = "Size", onClick = {})
    }
}

@Preview
@Composable
private fun ChipClickableSelectedPreview() {
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
