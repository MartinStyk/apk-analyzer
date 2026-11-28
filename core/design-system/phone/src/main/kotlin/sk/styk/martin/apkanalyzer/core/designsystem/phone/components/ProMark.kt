package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

private const val STAR_SCALE = 0.68f

@Composable
fun ProMark(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    inverted: Boolean = false,
) {
    val discColor = if (inverted) AppTheme.colors.onProFill else AppTheme.colors.proFill
    val starColor = if (inverted) AppTheme.colors.proFill else AppTheme.colors.onProFill
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(discColor, CircleShape),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.Star,
            contentDescription = null,
            tint = starColor,
            modifier = Modifier.size(size * STAR_SCALE),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProMarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            ProMark(size = 14.dp)
            ProMark(size = 22.dp)
            Box(modifier = Modifier.background(AppTheme.colors.proFill).padding(4.dp)) {
                ProMark(size = 18.dp, inverted = true)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF161618)
@Composable
private fun ProMarkDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            ProMark(size = 14.dp)
            ProMark(size = 22.dp)
        }
    }
}
