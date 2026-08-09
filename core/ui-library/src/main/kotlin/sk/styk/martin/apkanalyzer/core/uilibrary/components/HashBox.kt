package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import java.util.Locale

@Composable
fun HashBox(
    value: String,
    modifier: Modifier = Modifier,
    copyContentDescription: String? = null,
    onCopy: (() -> Unit)? = null,
) {
    val copyModifier = if (onCopy == null) {
        Modifier
    } else {
        Modifier
            .combinedClickable(onClick = onCopy, onLongClick = onCopy)
            .semantics {
                copyContentDescription?.let { contentDescription = it }
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surfaceVariant)
            .then(copyModifier),
    ) {
        Text(
            text = value.toDisplayFingerprint(),
            style = AppTheme.typography.monospace,
            color = AppTheme.colors.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    top = 12.dp,
                    end = if (onCopy == null) 12.dp else 52.dp,
                    bottom = 12.dp,
                ),
        )
        if (onCopy != null) {
            Icon(
                imageVector = ApkAnalyzerIcons.Copy,
                contentDescription = null,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(18.dp),
            )
        }
    }
}

private fun String.toDisplayFingerprint(): String = replace(":", "")
    .uppercase(Locale.ROOT)
    .chunked(2)
    .joinToString(":\u200B")

@Preview
@Composable
private fun HashBoxDefaultPreview() {
    ApkAnalyzerTheme {
        HashBox(
            value = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4",
            onCopy = {},
        )
    }
}

@Preview
@Composable
private fun HashBoxDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        HashBox(
            value = "38:91:8A:45:3D:07:19:93:54:F8:B1:9A:7A:6D:2B:EC:96:5A:21:22",
        )
    }
}
