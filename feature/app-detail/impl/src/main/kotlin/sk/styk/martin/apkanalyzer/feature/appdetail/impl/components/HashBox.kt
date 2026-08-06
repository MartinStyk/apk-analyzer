package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes

@Composable
internal fun HashBox(
    label: String,
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

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.CardShape)
                .background(AppTheme.colors.surfaceVariant)
                .then(copyModifier),
        ) {
            Text(
                text = value,
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
}

@Preview
@Composable
private fun HashBoxPreview() {
    ApkAnalyzerTheme {
        HashBox(
            label = "SHA-256",
            value = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4",
            onCopy = {},
        )
    }
}
