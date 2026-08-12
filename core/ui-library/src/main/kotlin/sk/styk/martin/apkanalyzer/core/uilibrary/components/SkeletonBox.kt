package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.shimmer
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.surfaceVariant,
    highlightColor: Color = AppTheme.colors.surface,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .shimmer(surfaceColor = color, highlightColor = highlightColor),
    )
}
