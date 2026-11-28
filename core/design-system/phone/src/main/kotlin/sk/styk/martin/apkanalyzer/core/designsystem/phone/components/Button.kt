package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
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
    containerColor: Color = AppTheme.colors.primary,
    contentColor: Color = AppTheme.colors.onPrimary,
    leadingIcon: ImageVector? = null,
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        }
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
private fun ButtonProPreview() {
    ApkAnalyzerTheme {
        Button(
            text = "Unlock Pro",
            onClick = {},
            leadingIcon = ApkAnalyzerIcons.Star,
            containerColor = AppTheme.colors.pro,
            contentColor = AppTheme.colors.proContainer,
        )
    }
}

@Preview
@Composable
private fun ButtonProDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        Button(
            text = "Unlock Pro",
            onClick = {},
            leadingIcon = ApkAnalyzerIcons.Star,
            containerColor = AppTheme.colors.pro,
            contentColor = AppTheme.colors.proContainer,
        )
    }
}

@Preview
@Composable
private fun ButtonProDisabledPreview() {
    ApkAnalyzerTheme {
        Button(
            text = "Unlock Pro",
            onClick = {},
            enabled = false,
            leadingIcon = ApkAnalyzerIcons.Star,
            containerColor = AppTheme.colors.pro,
            contentColor = AppTheme.colors.proContainer,
        )
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
