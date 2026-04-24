package sk.styk.martin.apkanalyzer.core.uilibrary.components

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
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

enum class IconButtonStyle {
    Standard,
    StandardMuted,
    Filled,
    Outlined,
}

@Composable
fun IconButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Standard,
) {
    when (style) {
        IconButtonStyle.Standard -> {
            IconButton(onClick = onClick, modifier = modifier) {
                Icon(imageVector = imageVector, contentDescription = null)
            }
        }

        IconButtonStyle.StandardMuted -> {
            IconButton(
                onClick = onClick,
                modifier = modifier,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = AppTheme.colors.onSurfaceVariant,
                ),
            ) {
                Icon(imageVector = imageVector, contentDescription = null)
            }
        }

        IconButtonStyle.Filled -> {
            FilledIconButton(
                onClick = onClick,
                modifier = modifier,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AppTheme.colors.surfaceVariant,
                    contentColor = AppTheme.colors.onBackground,
                ),
            ) {
                Icon(imageVector = imageVector, contentDescription = null)
            }
        }

        IconButtonStyle.Outlined -> {
            OutlinedIconButton(onClick = onClick, modifier = modifier) {
                Icon(imageVector = imageVector, contentDescription = null)
            }
        }
    }
}

@Preview
@Composable
private fun IconButtonStandardPreview() {
    ApkAnalyzerTheme {
        IconButton(imageVector = Icons.Filled.Star, onClick = {}, style = IconButtonStyle.Standard)
    }
}

@Preview
@Composable
private fun IconButtonFilledPreview() {
    ApkAnalyzerTheme {
        IconButton(imageVector = Icons.Filled.Star, onClick = {}, style = IconButtonStyle.Filled)
    }
}

@Preview
@Composable
private fun IconButtonOutlinedPreview() {
    ApkAnalyzerTheme {
        IconButton(imageVector = Icons.Filled.Star, onClick = {}, style = IconButtonStyle.Outlined)
    }
}
