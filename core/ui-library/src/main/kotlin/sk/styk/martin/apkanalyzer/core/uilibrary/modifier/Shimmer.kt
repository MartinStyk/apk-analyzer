package sk.styk.martin.apkanalyzer.core.uilibrary.modifier

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun Modifier.shimmer(): Modifier {
    val surface = AppTheme.colors.surface
    val highlight = AppTheme.colors.surfaceVariant

    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_progress",
    )

    return drawWithContent {
        drawContent()
        val width = size.width
        val height = size.height
        val shimmerWidth = width * 0.4f
        val startX = width * progress * 1.4f - shimmerWidth

        val brush = Brush.linearGradient(
            colors = listOf(surface, highlight, highlight, surface),
            start = Offset(startX, 0f),
            end = Offset(startX + shimmerWidth, height),
        )
        drawRect(brush = brush, size = size)
    }
}
