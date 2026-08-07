package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.ImmutableList
import sk.styk.martin.apkanalyzer.core.uilibrary.animation.NAV_DURATION_STANDARD

val navigationBarContentPadding: Dp = 80.dp

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
    BottomAppBarVisibility(
        isVisible = isVisible,
        modifier = modifier.fillMaxWidth(),
    ) {
        BottomAppBar {
            items.forEach { item ->
                val isSelected = item.navKey == selectedKey
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelectKey(item.navKey) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(id = item.title),
                        )
                    },
                    label = {
                        Text(stringResource(item.title))
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomAppBarVisibility(
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
