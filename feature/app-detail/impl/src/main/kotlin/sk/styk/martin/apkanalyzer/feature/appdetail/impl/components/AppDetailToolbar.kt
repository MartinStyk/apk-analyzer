package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.uilibrary.components.AppIcon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.ChipVariant
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.CollapsingToolbarState
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.AppDetailState
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import kotlin.math.roundToInt

private val BACK_BUTTON_SIZE = 48.dp
private val TOOLBAR_PADDING_VERTICAL = 8.dp
private val TOOLBAR_PADDING_START = 4.dp
private val ICON_SIZE_EXPANDED = 96.dp
private val ICON_SIZE_COLLAPSED = 32.dp
private val ICON_CONTAINER_PADDING = 8.dp
private val ICON_CONTAINER_SIZE = ICON_SIZE_EXPANDED + ICON_CONTAINER_PADDING * 2
private val ICON_SHADOW_EXPANDED = 4.dp
private val ICON_CORNER_EXPANDED = 16.dp
private val ICON_CORNER_COLLAPSED = 8.dp
private val APP_NAME_HORIZONTAL_MARGIN = 16.dp
private val ICON_COLLAPSE_SCALE = ICON_SIZE_COLLAPSED.value / ICON_SIZE_EXPANDED.value
private val ICON_PIVOT_FRACTION = ICON_CONTAINER_PADDING.value / ICON_CONTAINER_SIZE.value
private val FADING_CONTENT_RISE = ICON_CONTAINER_SIZE - ICON_SIZE_COLLAPSED

@Composable
internal fun AppDetailToolbar(
    state: AppDetailState.Loaded,
    appReference: AppReference,
    collapsingState: CollapsingToolbarState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var fadingContentHeight by remember { mutableStateOf(60.dp) }

    val appNameStyle = AppTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = AppTheme.typography.headlineMedium.fontSize,
    )
    val appNameCollapseScale = AppTheme.typography.titleLarge.fontSize.value /
        AppTheme.typography.headlineMedium.fontSize.value

    val backButtonEndX = TOOLBAR_PADDING_START + BACK_BUTTON_SIZE
    val backButtonCenterY = TOOLBAR_PADDING_VERTICAL + BACK_BUTTON_SIZE / 2

    val expandedIconY = TOOLBAR_PADDING_VERTICAL + BACK_BUTTON_SIZE + 16.dp
    val collapsedIconX = backButtonEndX - ICON_CONTAINER_PADDING
    val collapsedIconY = backButtonCenterY - ICON_SIZE_COLLAPSED / 2 - ICON_CONTAINER_PADDING

    val expandedAppNameY = expandedIconY + ICON_CONTAINER_SIZE + 12.dp
    val collapsedAppNameX = backButtonEndX + ICON_SIZE_COLLAPSED + 8.dp

    val fadingContentY = expandedAppNameY + 38.dp

    val expandedTotalHeight = fadingContentY + fadingContentHeight
    val collapsedTotalHeight = TOOLBAR_PADDING_VERTICAL * 2 + BACK_BUTTON_SIZE

    LaunchedEffect(expandedTotalHeight) {
        with(density) {
            collapsingState.maxCollapsePx = (expandedTotalHeight - collapsedTotalHeight).toPx()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
            .layout { measurable, constraints ->
                val expandedHeight = expandedTotalHeight.roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(minHeight = expandedHeight, maxHeight = expandedHeight),
                )
                val height = lerp(expandedHeight, collapsedTotalHeight.roundToPx(), collapsingState.progress)
                layout(placeable.width, height) { placeable.place(0, 0) }
            },
    ) {
        IconButton(
            imageVector = ApkAnalyzerIcons.Back,
            onClick = onBack,
            modifier = Modifier.offset(x = TOOLBAR_PADDING_START, y = TOOLBAR_PADDING_VERTICAL),
            contentDescription = stringResource(R.string.content_description_back),
        )

        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val parentWidth = constraints.maxWidth
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    layout(placeable.width, placeable.height) {
                        val progress = collapsingState.progress
                        val x = lerp((parentWidth - placeable.width) / 2f, collapsedIconX.toPx(), progress)
                        val y = lerp(expandedIconY.toPx(), collapsedIconY.toPx(), progress)
                        placeable.place(x.roundToInt(), y.roundToInt())
                    }
                }
                .graphicsLayer {
                    val progress = collapsingState.progress
                    val scale = lerp(1f, ICON_COLLAPSE_SCALE, progress)
                    val onScreenCorner = lerp(ICON_CORNER_EXPANDED.toPx(), ICON_CORNER_COLLAPSED.toPx(), progress)
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(ICON_PIVOT_FRACTION, ICON_PIVOT_FRACTION)
                    shadowElevation = lerp(ICON_SHADOW_EXPANDED.toPx(), 0f, progress)
                    shape = RoundedCornerShape(onScreenCorner / scale)
                    clip = true
                }
                .background(AppTheme.colors.background)
                .padding(ICON_CONTAINER_PADDING),
        ) {
            AppIcon(
                source = appReference,
                size = ICON_SIZE_EXPANDED,
            )
        }

        Text(
            text = state.appName,
            style = appNameStyle,
            color = AppTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .layout { measurable, constraints ->
                    val parentWidth = constraints.maxWidth
                    val placeable = measurable.measure(
                        constraints.copy(
                            minWidth = 0,
                            maxWidth = parentWidth - APP_NAME_HORIZONTAL_MARGIN.roundToPx() * 2,
                        ),
                    )
                    layout(placeable.width, placeable.height) {
                        val progress = collapsingState.progress
                        val x = lerp((parentWidth - placeable.width) / 2f, collapsedAppNameX.toPx(), progress)
                        val y = lerp(
                            expandedAppNameY.toPx(),
                            backButtonCenterY.toPx() - placeable.height / 2f,
                            progress,
                        )
                        placeable.place(x.roundToInt(), y.roundToInt())
                    }
                }
                .graphicsLayer {
                    val scale = lerp(1f, appNameCollapseScale, collapsingState.progress)
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = fadingContentY)
                .graphicsLayer {
                    val progress = collapsingState.progress
                    alpha = 1f - progress
                    translationY = lerp(0f, -FADING_CONTENT_RISE.toPx(), progress)
                }
                .onSizeChanged { size ->
                    fadingContentHeight = with(density) { size.height.toDp() }
                },
        ) {
            Text(
                text = state.packageName.value,
                style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.badges.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    state.badges.forEach { badge ->
                        Chip(
                            label = stringResource(badge.labelRes()),
                            variant = badge.chipVariant(),
                            leadingIcon = badge.icon(),
                        )
                    }
                }
            }
        }
    }
}

private fun AppDetailBadge.chipVariant(): ChipVariant = when (this) {
    AppDetailBadge.Sideloaded,
    AppDetailBadge.DangerousPermissions,
    AppDetailBadge.Unused,
    -> ChipVariant.Warning

    AppDetailBadge.Large -> ChipVariant.Tonal

    AppDetailBadge.System,
    AppDetailBadge.GooglePlay,
    AppDetailBadge.RecentlyInstalled,
    AppDetailBadge.RecentlyUpdated,
    AppDetailBadge.RecentlyUsed,
    -> ChipVariant.Default
}

private fun AppDetailBadge.labelRes(): Int = when (this) {
    AppDetailBadge.Sideloaded -> R.string.app_detail_badge_sideloaded
    AppDetailBadge.DangerousPermissions -> R.string.app_detail_badge_dangerous_permissions
    AppDetailBadge.Unused -> R.string.app_detail_badge_unused
    AppDetailBadge.Large -> R.string.app_detail_badge_large
    AppDetailBadge.System -> R.string.app_detail_badge_system
    AppDetailBadge.GooglePlay -> R.string.app_detail_badge_google_play
    AppDetailBadge.RecentlyInstalled -> R.string.app_detail_badge_recently_installed
    AppDetailBadge.RecentlyUpdated -> R.string.app_detail_badge_recently_updated
    AppDetailBadge.RecentlyUsed -> R.string.app_detail_badge_recently_used
}

private fun AppDetailBadge.icon(): ImageVector = when (this) {
    AppDetailBadge.Sideloaded -> ApkAnalyzerIcons.Warning
    AppDetailBadge.DangerousPermissions -> ApkAnalyzerIcons.DangerousPermissions
    AppDetailBadge.Unused -> ApkAnalyzerIcons.HourglassEmpty
    AppDetailBadge.Large -> ApkAnalyzerIcons.DataUsage
    AppDetailBadge.System -> ApkAnalyzerIcons.Android
    AppDetailBadge.GooglePlay -> ApkAnalyzerIcons.PlayArrow
    AppDetailBadge.RecentlyInstalled -> ApkAnalyzerIcons.NewReleases
    AppDetailBadge.RecentlyUpdated -> ApkAnalyzerIcons.Sync
    AppDetailBadge.RecentlyUsed -> ApkAnalyzerIcons.History
}
