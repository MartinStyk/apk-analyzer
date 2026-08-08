@file:Suppress("MatchingDeclarationName")

package sk.styk.martin.apkanalyzer.core.uilibrary.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

@Stable
class CollapsingToolbarState {
    var maxCollapsePx by mutableFloatStateOf(0f)
    var collapseOffsetPx by mutableFloatStateOf(0f)
        internal set

    val progress: Float by derivedStateOf {
        if (maxCollapsePx > 0f) (collapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f) else 0f
    }

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            if (delta < 0f) {
                val previous = collapseOffsetPx
                collapseOffsetPx = (collapseOffsetPx - delta).coerceIn(0f, maxCollapsePx)
                val consumed = collapseOffsetPx - previous
                if (consumed > 0f) return Offset(0f, -consumed)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val delta = available.y
            if (delta > 0f) {
                val previous = collapseOffsetPx
                collapseOffsetPx = (collapseOffsetPx - delta).coerceIn(0f, maxCollapsePx)
                val expandConsumed = previous - collapseOffsetPx
                if (expandConsumed > 0f) return Offset(0f, expandConsumed)
            }
            return Offset.Zero
        }
    }
}

@Composable
fun rememberCollapsingToolbarState(): CollapsingToolbarState = remember { CollapsingToolbarState() }

fun Modifier.collapsingToolbarScroll(state: CollapsingToolbarState): Modifier = this.nestedScroll(state.nestedScrollConnection)
