package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(text = confirmText, onClick = onConfirm) },
        dismissButton = { TextButton(text = dismissText, onClick = onDismiss) },
        title = { Text(text = title, style = AppTheme.typography.headlineSmall) },
        text = { Text(text = message, style = AppTheme.typography.bodyMedium) },
        shape = Shapes.CardShape,
        containerColor = AppTheme.colors.surface,
        titleContentColor = AppTheme.colors.onSurface,
        textContentColor = AppTheme.colors.onSurface,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    ApkAnalyzerTheme {
        ConfirmDialog(
            title = "Stop export?",
            message = "The unfinished file will be deleted. Your selections will be kept.",
            confirmText = "Stop and go back",
            dismissText = "Keep exporting",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ConfirmDialogDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        ConfirmDialog(
            title = "Stop export?",
            message = "The unfinished file will be deleted. Your selections will be kept.",
            confirmText = "Stop and go back",
            dismissText = "Keep exporting",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
