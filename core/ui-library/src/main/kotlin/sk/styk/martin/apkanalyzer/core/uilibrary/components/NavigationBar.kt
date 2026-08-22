package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.uilibrary.R
import sk.styk.martin.apkanalyzer.core.uilibrary.animation.NAV_DURATION_STANDARD
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import kotlin.math.roundToInt

private val NavigationPillHeight: Dp = 56.dp
private val NavigationPillMargin: Dp = 16.dp
private val NavigationPillItemPadding: Dp = 4.dp
private val NavigationPillItemSpacing: Dp = 8.dp
private val NavigationPillItemHorizontalPadding: Dp = 16.dp
private val NavigationPillIconSize: Dp = 20.dp
private val NavigationPillElevation: Dp = 8.dp
private val NavigationPillBorderWidth: Dp = 1.dp

val navigationBarContentPadding: Dp = NavigationPillHeight + NavigationPillMargin

@Stable
data class NavigationBarItem(
    val navKey: NavKey,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val title: Int,
)

@Composable
fun NavigationBar(
    items: ImmutableList<NavigationBarItem>,
    selectedKey: NavKey,
    isVisible: Boolean,
    onSelectKey: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBarVisibility(
        isVisible = isVisible,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = NavigationPillMargin,
                    end = NavigationPillMargin,
                    bottom = NavigationPillMargin,
                ),
            contentAlignment = Alignment.Center,
        ) {
            NavigationPill(
                items = items,
                selectedKey = selectedKey,
                onSelectKey = onSelectKey,
            )
        }
    }
}

@Composable
private fun NavigationPill(
    items: ImmutableList<NavigationBarItem>,
    selectedKey: NavKey,
    onSelectKey: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragState = remember {
        AnchoredDraggableState(
            initialValue = items.indexOfFirst { it.navKey == selectedKey }.coerceAtLeast(0),
        )
    }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(dragState)
    val currentSelectedKey by rememberUpdatedState(selectedKey)
    val currentOnSelectKey by rememberUpdatedState(onSelectKey)

    LaunchedEffect(items) {
        launch {
            snapshotFlow { items.indexOfFirst { it.navKey == currentSelectedKey }.coerceAtLeast(0) }
                .drop(1)
                .collect { dragState.animateTo(it) }
        }
        snapshotFlow { dragState.settledValue }
            .drop(1)
            .collect { settledIndex ->
                val settledKey = items.getOrNull(settledIndex)?.navKey
                if (settledKey != null && settledKey != currentSelectedKey) {
                    currentOnSelectKey(settledKey)
                }
            }
    }

    Box(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .height(NavigationPillHeight)
            .shadow(elevation = NavigationPillElevation, shape = CircleShape)
            .background(color = AppTheme.colors.surfaceVariant, shape = CircleShape)
            .border(width = NavigationPillBorderWidth, color = AppTheme.colors.pillBorder, shape = CircleShape)
            .padding(NavigationPillItemPadding)
            .onSizeChanged { size ->
                val itemWidth = size.width.toFloat() / items.size
                dragState.updateAnchors(
                    DraggableAnchors {
                        items.indices.forEach { index -> index at index * itemWidth }
                    },
                )
            }
            .anchoredDraggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                flingBehavior = flingBehavior,
            )
            .selectableGroup(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f / items.size)
                .fillMaxHeight()
                .offset {
                    val offset = dragState.offset
                    IntOffset(x = if (offset.isNaN()) 0 else offset.roundToInt(), y = 0)
                }
                .background(color = AppTheme.colors.primaryContainer, shape = CircleShape),
        )

        val activeIndex = dragState.targetValue
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                NavigationPillItem(
                    item = item,
                    isSelected = index == activeIndex,
                    onClick = { onSelectKey(item.navKey) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun NavigationPillItem(
    item: NavigationBarItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant,
        animationSpec = tween(durationMillis = NAV_DURATION_STANDARD),
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = NavigationPillItemHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NavigationPillItemSpacing, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            tint = contentColor,
            modifier = Modifier.size(NavigationPillIconSize),
        )
        Text(
            text = stringResource(item.title),
            style = AppTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NavigationBarVisibility(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = NAV_DURATION_STANDARD,
                easing = LinearOutSlowInEasing,
            ),
            initialOffsetY = { it },
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = NAV_DURATION_STANDARD,
                easing = LinearOutSlowInEasing,
            ),
            initialScale = 0.96f,
            transformOrigin = TransformOrigin(0.5f, 1f),
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 180,
                delayMillis = 100,
            ),
        ),
        exit = slideOutVertically(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutLinearInEasing,
            ),
            targetOffsetY = { it },
        ) + scaleOut(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutLinearInEasing,
            ),
            targetScale = 0.96f,
            transformOrigin = TransformOrigin(0.5f, 1f),
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 100,
                delayMillis = 200,
            ),
        ),
    ) {
        content()
    }
}

private data object PreviewAppsKey : NavKey

private data object PreviewBrowseKey : NavKey

private val previewItems = persistentListOf(
    NavigationBarItem(
        navKey = PreviewAppsKey,
        selectedIcon = ApkAnalyzerIcons.Apps,
        unselectedIcon = ApkAnalyzerIcons.AppsBorder,
        title = R.string.content_description_back,
    ),
    NavigationBarItem(
        navKey = PreviewBrowseKey,
        selectedIcon = ApkAnalyzerIcons.Browse,
        unselectedIcon = ApkAnalyzerIcons.BrowseBorder,
        title = R.string.content_description_selected,
    ),
)

@Preview
@Composable
private fun NavigationBarFirstSelectedPreview() {
    ApkAnalyzerTheme {
        NavigationBar(
            items = previewItems,
            selectedKey = PreviewAppsKey,
            isVisible = true,
            onSelectKey = {},
        )
    }
}

@Preview
@Composable
private fun NavigationBarSecondSelectedPreview() {
    ApkAnalyzerTheme {
        NavigationBar(
            items = previewItems,
            selectedKey = PreviewBrowseKey,
            isVisible = true,
            onSelectKey = {},
        )
    }
}
