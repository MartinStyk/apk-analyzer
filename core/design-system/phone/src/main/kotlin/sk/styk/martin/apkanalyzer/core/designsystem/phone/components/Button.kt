package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.TextButton as MaterialTextButton

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.primary,
            contentColor = AppTheme.colors.onPrimary,
        ),
    ) {
        Text(text = text, style = AppTheme.typography.labelLarge)
    }
}

@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = BorderStroke(width = 1.dp, color = AppTheme.colors.onSurfaceVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.primary),
    ) {
        Text(text = text, style = AppTheme.typography.labelLarge)
    }
}

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MaterialTextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.primary),
    ) {
        Text(text = text, style = AppTheme.typography.labelLarge)
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
private fun ButtonDisabledPreview() {
    ApkAnalyzerTheme {
        Button(text = "Next", onClick = {}, enabled = false)
    }
}

@Preview
@Composable
private fun OutlinedButtonPreview() {
    ApkAnalyzerTheme {
        OutlinedButton(text = "Share", onClick = {})
    }
}

@Preview
@Composable
private fun TextButtonPreview() {
    ApkAnalyzerTheme {
        TextButton(text = "Reset", onClick = {})
    }
}
