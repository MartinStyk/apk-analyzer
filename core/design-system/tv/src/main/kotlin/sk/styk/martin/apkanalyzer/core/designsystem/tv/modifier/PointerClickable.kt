package sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusRequesterModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.LocalApkAnalyzerColors

fun Modifier.pointerClickable(shape: Shape, onClick: () -> Unit): Modifier = this then PointerClickableElement(shape, onClick)

private data class PointerClickableElement(val shape: Shape, val onClick: () -> Unit) : ModifierNodeElement<PointerClickableNode>() {
    override fun create() = PointerClickableNode(shape, onClick)

    override fun update(node: PointerClickableNode) {
        node.onClick = onClick
        if (node.shape != shape) {
            node.shape = shape
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pointerClickable"
        properties["shape"] = shape
        properties["onClick"] = onClick
    }
}

private class PointerClickableNode(var shape: Shape, var onClick: () -> Unit) :
    DelegatingNode(),
    FocusRequesterModifierNode,
    FocusEventModifierNode,
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {

    private var hovered = false
    private var focused = false

    init {
        delegate(
            SuspendingPointerInputModifierNode {
                coroutineScope {
                    launch {
                        detectTapGestures(
                            onTap = {
                                requestFocus()
                                onClick()
                            },
                        )
                    }
                    launch {
                        awaitPointerEventScope {
                            while (true) {
                                when (awaitPointerEvent().type) {
                                    PointerEventType.Enter -> updateHovered(true)
                                    PointerEventType.Exit -> updateHovered(false)
                                }
                            }
                        }
                    }
                }
            },
        )
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (focused != focusState.hasFocus) {
            focused = focusState.hasFocus
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (hovered && !focused) {
            val strokeWidth = HoverBorderWidth.toPx()
            inset(strokeWidth / 2) {
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = currentValueOf(LocalApkAnalyzerColors).onSurfaceVariant,
                    style = Stroke(width = strokeWidth),
                )
            }
        }
    }

    private fun updateHovered(value: Boolean) {
        if (hovered != value) {
            hovered = value
            invalidateDraw()
        }
    }
}

private val HoverBorderWidth: Dp = 1.5.dp
