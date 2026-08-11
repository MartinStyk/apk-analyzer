@file:Suppress("MatchingDeclarationName")

package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Immutable
data class BarChartSegment(val value: Float, val color: Color)

private const val REVEAL_DURATION_MS = 700
private const val SEGMENT_GAP_DP = 3

@Composable
fun SegmentedBarChart(
    segments: ImmutableList<BarChartSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
) {
    val trackColor = AppTheme.colors.surfaceVariant
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(segments) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, animationSpec = tween(durationMillis = REVEAL_DURATION_MS, easing = FastOutSlowInEasing))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val total = segments.sumOf { it.value.toDouble() }.toFloat()
        val gapPx = SEGMENT_GAP_DP.dp.toPx()
        val minWidthPx = size.height
        val barPath = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(size.height / 2)))
        }
        clipPath(barPath) {
            drawRect(color = trackColor, size = size)
            clipRect(right = size.width * reveal.value) {
                var startX = 0f
                segments.forEach { segment ->
                    val fraction = if (total > 0f) segment.value / total else 0f
                    val segmentWidth = size.width * fraction
                    if (segmentWidth > 0f) {
                        val drawWidth = (segmentWidth - gapPx).coerceAtLeast(minWidthPx)
                        drawRect(
                            color = segment.color,
                            topLeft = Offset(startX, 0f),
                            size = Size(drawWidth, size.height),
                        )
                        startX += maxOf(segmentWidth, drawWidth + gapPx)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SegmentedBarChartPreview() {
    ApkAnalyzerTheme {
        SegmentedBarChart(
            segments = persistentListOf(
                BarChartSegment(value = 620f, color = AppTheme.colors.primary),
                BarChartSegment(value = 240f, color = AppTheme.colors.secondary),
                BarChartSegment(value = 90f, color = AppTheme.colors.tertiary),
            ),
        )
    }
}

@Preview
@Composable
private fun SegmentedBarChartDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        SegmentedBarChart(
            segments = persistentListOf(
                BarChartSegment(value = 620f, color = AppTheme.colors.primary),
                BarChartSegment(value = 240f, color = AppTheme.colors.secondary),
                BarChartSegment(value = 90f, color = AppTheme.colors.tertiary),
            ),
        )
    }
}
