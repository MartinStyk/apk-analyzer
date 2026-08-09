package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun LabeledHashBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    copyContentDescription: String? = null,
    onCopy: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        HashBox(
            value = value,
            copyContentDescription = copyContentDescription,
            onCopy = onCopy,
        )
    }
}

@Preview
@Composable
private fun LabeledHashBoxDefaultPreview() {
    ApkAnalyzerTheme {
        LabeledHashBox(
            label = "SHA-256",
            value = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4",
            onCopy = {},
        )
    }
}

@Preview
@Composable
private fun LabeledHashBoxDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        LabeledHashBox(
            label = "SHA-1",
            value = "38:91:8A:45:3D:07:19:93:54:F8:B1:9A:7A:6D:2B:EC:96:5A:21:22",
        )
    }
}
