package sk.styk.martin.apkanalyzer.core.designsystem.tv.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.tv.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinimalFocusScroll(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBringIntoViewSpec provides MinimalScrollSpec, content = content)
}

@OptIn(ExperimentalFoundationApi::class)
private val MinimalScrollSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float {
        val margin = containerSize * EDGE_MARGIN_FRACTION
        val leadingEdge = offset - margin
        val trailingEdge = offset + size + margin
        return when {
            leadingEdge >= 0f && trailingEdge <= containerSize -> 0f
            leadingEdge < 0f || size + 2 * margin > containerSize -> leadingEdge
            else -> trailingEdge - containerSize
        }
    }
}
private const val EDGE_MARGIN_FRACTION = 0.06f

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MinimalFocusScrollPreview() {
    ApkAnalyzerTvTheme {
        MinimalFocusScroll {
            Text(text = "YouTube")
        }
    }
}
