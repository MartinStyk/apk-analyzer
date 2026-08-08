@file:Suppress("MatchingDeclarationName")

package sk.styk.martin.apkanalyzer.core.uilibrary.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Stable
class CollapsingHeaderState {
    var headerHeightPx by mutableFloatStateOf(0f)
        internal set
    var headerOffsetPx by mutableFloatStateOf(0f)
        internal set

    val progress: Float
        get() = if (headerHeightPx > 0f) (-headerOffsetPx / headerHeightPx).coerceIn(0f, 1f) else 0f

    val headerOffset: IntOffset
        get() = IntOffset(0, headerOffsetPx.roundToInt())

    val contentOffset: IntOffset
        get() = IntOffset(0, (headerHeightPx + headerOffsetPx).roundToInt())

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            val previous = headerOffsetPx
            headerOffsetPx = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
            return Offset(0f, headerOffsetPx - previous)
        }
    }
}

@Composable
fun rememberCollapsingHeaderState(): CollapsingHeaderState = remember { CollapsingHeaderState() }

fun Modifier.collapsingHeaderContainer(state: CollapsingHeaderState): Modifier = this.nestedScroll(state.nestedScrollConnection)

fun Modifier.collapsingHeader(state: CollapsingHeaderState): Modifier = this.onGloballyPositioned { state.headerHeightPx = it.size.height.toFloat() }
