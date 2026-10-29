package sk.styk.martin.apkanalyzer.core.designsystem.tv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.pointerClickable
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme

@Composable
fun ScrollablePanel(
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
    contentPadding: PaddingValues = PaddingValues(28.dp),
    spacing: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(resetKey) {
        scrollState.scrollTo(0)
    }

    Box(
        modifier = modifier
            .background(color = AppTheme.colors.surface, shape = PanelShape)
            .border(
                border = BorderStroke(width = 2.dp, color = if (isFocused) AppTheme.colors.primary else Color.Transparent),
                shape = PanelShape,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (onClick != null && event.key in ClickKeys) {
                        if (event.type == KeyEventType.KeyUp) onClick()
                        return@onPreviewKeyEvent true
                    }
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val step = when (event.key) {
                        Key.DirectionDown -> if (scrollState.canScrollForward) SCROLL_STEP else return@onPreviewKeyEvent false
                        Key.DirectionUp -> if (scrollState.canScrollBackward) -SCROLL_STEP else return@onPreviewKeyEvent false
                        else -> return@onPreviewKeyEvent false
                    }
                    coroutineScope.launch { scrollState.animateScrollBy(step) }
                    true
                }
                .then(if (onClick != null) Modifier.pointerClickable(PanelShape, onClick) else Modifier)
                .focusable(enabled = onClick != null || scrollState.maxValue > 0)
                .verticalScroll(scrollState)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
        if (scrollState.canScrollForward) {
            Icon(
                imageVector = ApkAnalyzerIcons.ArrowDropDown,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .background(color = AppTheme.colors.surfaceVariant, shape = CircleShape)
                    .size(32.dp),
            )
        }
    }
}

private val PanelShape = RoundedCornerShape(16.dp)
private const val SCROLL_STEP = 240f
private val ClickKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ScrollablePanelPreview() {
    ApkAnalyzerTvTheme {
        ScrollablePanel(modifier = Modifier.width(400.dp)) {
            Text(text = "About this certificate", style = AppTheme.typography.titleMedium)
            Text(
                text = "Self-signed certificates are normal for Android apps.",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}
