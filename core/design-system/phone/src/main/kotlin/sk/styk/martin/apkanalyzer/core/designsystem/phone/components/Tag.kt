package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes

@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    highlighted: Boolean = false,
    contentColor: Color = if (highlighted) AppTheme.colors.onSecondaryContainer else AppTheme.colors.onSurfaceVariant,
    containerColor: Color = if (highlighted) AppTheme.colors.secondaryContainer else AppTheme.colors.surfaceVariant,
    textStyle: TextStyle = AppTheme.typography.labelSmall,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(Shapes.CardShape)
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            style = textStyle,
            color = contentColor,
        )
    }
}

@Preview
@Composable
private fun TagPreview() {
    ApkAnalyzerTheme {
        Tag(text = "Exported")
    }
}

@Preview
@Composable
private fun TagHighlightedPreview() {
    ApkAnalyzerTheme {
        Tag(text = "Launcher", highlighted = true)
    }
}

@Preview
@Composable
private fun TagWithIconPreview() {
    ApkAnalyzerTheme {
        Tag(text = "App launcher entry", icon = ApkAnalyzerIcons.PlayArrow, highlighted = true)
    }
}
