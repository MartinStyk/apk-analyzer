package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

enum class IconButtonStyle {
    Standard,
    StandardMuted,
    Filled,
    Outlined,
    Highlighted,
}

@Suppress("LongParameterList")
@Composable
fun IconButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Standard,
    enabled: Boolean = true,
    contentDescription: String? = null,
    iconSize: Dp? = null,
) {
    val iconModifier = if (iconSize != null) Modifier.size(iconSize) else Modifier

    when (style) {
        IconButtonStyle.Standard -> {
            IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
                Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = iconModifier)
            }
        }

        IconButtonStyle.StandardMuted -> {
            IconButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = AppTheme.colors.onSurfaceVariant,
                ),
            ) {
                Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = iconModifier)
            }
        }

        IconButtonStyle.Filled -> {
            FilledIconButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AppTheme.colors.surfaceVariant,
                    contentColor = AppTheme.colors.onBackground,
                ),
            ) {
                Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = iconModifier)
            }
        }

        IconButtonStyle.Outlined -> {
            OutlinedIconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
                Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = iconModifier)
            }
        }

        IconButtonStyle.Highlighted -> {
            FilledIconButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AppTheme.colors.secondaryContainer,
                    contentColor = AppTheme.colors.onSecondaryContainer,
                ),
            ) {
                Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = iconModifier)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IconButtonStandardPreview() {
    ApkAnalyzerTheme {
        IconButton(imageVector = Icons.Filled.Star, onClick = {}, style = IconButtonStyle.Standard)
    }
}

@Preview(showBackground = true)
@Composable
private fun IconButtonStandardMutedPreview() {
    ApkAnalyzerTheme {
        IconButton(imageVector = Icons.Filled.Star, onClick = {}, style = IconButtonStyle.StandardMuted)
    }
}

@Preview(showBackground = true)
@Composable
private fun IconButtonFilledPreview() {
    ApkAnalyzerTheme {
        IconButton(imageVector = Icons.Filled.Star, onClick = {}, style = IconButtonStyle.Filled)
    }
}

@Preview(showBackground = true)
@Composable
private fun IconButtonOutlinedPreview() {
    ApkAnalyzerTheme {
        IconButton(imageVector = Icons.Filled.Star, onClick = {}, style = IconButtonStyle.Outlined)
    }
}

@Preview(showBackground = true)
@Composable
private fun IconButtonCompactPreview() {
    ApkAnalyzerTheme {
        IconButton(
            imageVector = Icons.Filled.Star,
            onClick = {},
            style = IconButtonStyle.Filled,
            iconSize = 18.dp,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IconButtonHighlightedPreview() {
    ApkAnalyzerTheme {
        IconButton(imageVector = Icons.Filled.Star, onClick = {}, style = IconButtonStyle.Highlighted)
    }
}
