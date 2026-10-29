package sk.styk.martin.apkanalyzer.core.designsystem.tv.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.rememberAppAccentColor
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.pointerClickable
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import kotlin.math.roundToInt
import androidx.tv.material3.Card as TvMaterialCard

val AppCardWidth: Dp = 180.dp

@Composable
fun AppCard(
    packageName: PackageName,
    applicationName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val reference = remember(packageName) { AppReference.InstalledPackage(packageName) }
    val accent by animateColorAsState(
        targetValue = rememberAppAccentColor(reference) ?: AppTheme.colors.surfaceVariant,
        animationSpec = tween(durationMillis = TINT_FADE_DURATION),
        label = "appCardTint",
    )
    TvMaterialCard(
        onClick = onClick,
        modifier = modifier.pointerClickable(CardShape, onClick),
        shape = CardDefaults.shape(shape = CardShape),
        colors = CardDefaults.colors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.onSurface,
            focusedContainerColor = AppTheme.colors.surfaceVariant,
            focusedContentColor = AppTheme.colors.onSurface,
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = FocusBorderWidth, color = AppTheme.colors.primary),
                shape = CardShape,
            ),
        ),
        scale = CardDefaults.scale(focusedScale = 1.05f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .minAspectRatio(TILE_ASPECT_RATIO)
                .background(Brush.linearGradient(listOf(accent.copy(alpha = TINT_START_ALPHA), accent.copy(alpha = TINT_END_ALPHA))))
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                AppIcon(source = reference, size = 36.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    if (description != null) {
                        Text(
                            text = description,
                            style = AppTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .background(color = AppTheme.colors.background.copy(alpha = PILL_ALPHA), shape = PillShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = applicationName, style = AppTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = packageName.value,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun Modifier.minAspectRatio(ratio: Float) = layout { measurable, constraints ->
    val minHeight = if (constraints.hasBoundedWidth) (constraints.maxWidth / ratio).roundToInt() else 0
    val placeable = measurable.measure(constraints.copy(minHeight = maxOf(constraints.minHeight, minHeight.coerceAtMost(constraints.maxHeight))))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private val CardShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(6.dp)
private const val TILE_ASPECT_RATIO = 16f / 9f
private const val TINT_START_ALPHA = 0.34f
private const val TINT_END_ALPHA = 0.08f
private const val PILL_ALPHA = 0.55f
private const val TINT_FADE_DURATION = 400

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AppCardPreview() {
    ApkAnalyzerTvTheme {
        AppCard(
            packageName = PackageName("com.google.android.youtube.tv"),
            applicationName = "YouTube",
            onClick = {},
            description = "142 MB",
            modifier = Modifier.width(AppCardWidth),
        )
    }
}
