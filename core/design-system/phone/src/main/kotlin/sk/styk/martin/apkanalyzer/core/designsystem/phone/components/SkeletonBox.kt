package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.shimmer
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

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

@Preview(showBackground = true)
@Composable
private fun SkeletonBoxPreview() {
    ApkAnalyzerTheme {
        SkeletonBox(modifier = Modifier.width(120.dp).height(14.dp))
    }
}
