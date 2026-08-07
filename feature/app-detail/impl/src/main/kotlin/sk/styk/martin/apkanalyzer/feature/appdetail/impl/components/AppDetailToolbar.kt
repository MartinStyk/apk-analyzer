package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.uilibrary.components.AppIcon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Chip
import sk.styk.martin.apkanalyzer.core.uilibrary.components.ChipVariant
import sk.styk.martin.apkanalyzer.core.uilibrary.components.IconButton
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.CollapsingToolbarState
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.util.lerp
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.AppDetailState
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R
import androidx.compose.ui.util.lerp as lerpFloat

private val BACK_BUTTON_SIZE = 48.dp
private val TOOLBAR_PADDING_VERTICAL = 8.dp
private val TOOLBAR_PADDING_START = 4.dp
private val ICON_SIZE_EXPANDED = 96.dp
private val ICON_SIZE_COLLAPSED = 32.dp

@Composable
internal fun AppDetailToolbar(
    state: AppDetailState.Loaded,
    appReference: AppReference,
    collapsingState: CollapsingToolbarState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val progress = collapsingState.progress
    var packageNameHeightDp by remember { mutableStateOf(20.dp) }
    val iconSize = lerp(ICON_SIZE_EXPANDED, ICON_SIZE_COLLAPSED, progress)
    val iconContainerPadding = lerp(8.dp, 0.dp, progress)
    val iconContainerSize = iconSize + iconContainerPadding * 2

    val backButtonEndX = TOOLBAR_PADDING_START + BACK_BUTTON_SIZE
    val backButtonCenterY = TOOLBAR_PADDING_VERTICAL + BACK_BUTTON_SIZE / 2

    val expandedIconY = TOOLBAR_PADDING_VERTICAL + BACK_BUTTON_SIZE + 16.dp
    val collapsedIconY = backButtonCenterY - ICON_SIZE_COLLAPSED / 2
    val iconY = lerp(expandedIconY, collapsedIconY, progress)

    val expandedAppNameStyle = AppTheme.typography.headlineMedium
    val collapsedAppNameStyle = AppTheme.typography.titleLarge
    val appNameFontSize = lerpFloat(
        expandedAppNameStyle.fontSize.value,
        collapsedAppNameStyle.fontSize.value,
        progress,
    ).sp

    val expandedAppNameY = expandedIconY + iconContainerSize + 12.dp
    val collapsedAppNameY = backButtonCenterY - 12.dp
    val appNameY = lerp(expandedAppNameY, collapsedAppNameY, progress)

    val packageNameY = expandedAppNameY + 38.dp
    val badgesRowHeight = if (state.badges.isNotEmpty()) 40.dp else 0.dp
    val badgesY = packageNameY + packageNameHeightDp + 8.dp

    val expandedTotalHeight = badgesY + badgesRowHeight
    val collapsedTotalHeight = TOOLBAR_PADDING_VERTICAL * 2 + BACK_BUTTON_SIZE
    val totalHeight = lerp(expandedTotalHeight, collapsedTotalHeight, progress)

    LaunchedEffect(expandedTotalHeight) {
        with(density) {
            collapsingState.maxCollapsePx = (expandedTotalHeight - collapsedTotalHeight).toPx()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .background(AppTheme.colors.background),
    ) {
        val parentWidth = maxWidth

        val expandedIconX = (parentWidth - iconContainerSize) / 2
        val collapsedIconX = backButtonEndX
        val iconX = lerp(expandedIconX, collapsedIconX, progress)

        val collapsedAppNameX = backButtonEndX + ICON_SIZE_COLLAPSED + 8.dp

        IconButton(
            imageVector = ApkAnalyzerIcons.Back,
            onClick = onBack,
            modifier = Modifier.offset(x = TOOLBAR_PADDING_START, y = TOOLBAR_PADDING_VERTICAL),
            contentDescription = stringResource(R.string.content_description_back),
        )

        Box(
            modifier = Modifier
                .offset(x = iconX, y = iconY)
                .shadow(
                    elevation = lerp(4.dp, 0.dp, progress),
                    shape = RoundedCornerShape(lerp(16.dp, 8.dp, progress)),
                )
                .background(
                    color = AppTheme.colors.background,
                    shape = RoundedCornerShape(lerp(16.dp, 8.dp, progress)),
                )
                .padding(iconContainerPadding),
        ) {
            AppIcon(
                source = appReference,
                size = iconSize,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = appNameY)
                .widthIn(max = lerp(parentWidth - 32.dp, parentWidth - collapsedAppNameX - 16.dp, progress))
                .graphicsLayer {
                    val collapsedXPx = with(density) { collapsedAppNameX.toPx() }
                    val parentWidthPx = with(density) { parentWidth.toPx() }
                    val currentLeftEdge = (parentWidthPx - size.width) / 2f
                    translationX = lerpFloat(0f, collapsedXPx - currentLeftEdge, progress)
                },
        ) {
            Text(
                text = state.appName,
                style = AppTheme.typography.titleLarge.copy(
                    fontWeight = lerp(FontWeight.Bold, FontWeight.Normal, progress),
                    fontSize = appNameFontSize,
                ),
                color = AppTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = state.packageName,
            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = packageNameY)
                .graphicsLayer { alpha = (1f - progress).coerceIn(0f, 1f) }
                .onSizeChanged { size ->
                    packageNameHeightDp = with(density) { size.height.toDp() }
                },
        )

        if (state.badges.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .offset(y = badgesY)
                    .graphicsLayer { alpha = (1f - progress).coerceIn(0f, 1f) }
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
