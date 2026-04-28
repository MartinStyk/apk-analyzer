package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.TextButton as MaterialTextButton

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.primary,
            contentColor = AppTheme.colors.onPrimary,
        ),
    ) {
        Text(text = text, style = AppTheme.typography.labelLarge)
    }
}

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTextButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(text = text, style = AppTheme.typography.labelLarge, color = AppTheme.colors.primary)
    }
}

@Preview
@Composable
private fun ButtonPreview() {
    ApkAnalyzerTheme {
        Button(text = "Apply", onClick = {})
    }
}

@Preview
@Composable
private fun TextButtonPreview() {
    ApkAnalyzerTheme {
        TextButton(text = "Reset", onClick = {})
    }
}
