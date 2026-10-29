package sk.styk.martin.apkanalyzer.core.designsystem.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.SurfaceDefaults
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.pointerClickable
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import androidx.tv.material3.Surface as TvMaterialSurface

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        ConfirmDialogContent(
            title = title,
            text = text,
            confirmLabel = confirmLabel,
            dismissLabel = dismissLabel,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ConfirmDialogContent(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val confirmFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { confirmFocusRequester.requestFocus() }

    TvMaterialSurface(
        shape = RoundedCornerShape(16.dp),
        colors = SurfaceDefaults.colors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = title, style = AppTheme.typography.headlineSmall)
            Text(text = text, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.pointerClickable(CircleShape, onDismiss)) {
                    Text(text = dismissLabel, style = AppTheme.typography.labelLarge)
                }
                Button(onClick = onConfirm, modifier = Modifier.focusRequester(confirmFocusRequester).pointerClickable(CircleShape, onConfirm)) {
                    Text(text = confirmLabel, style = AppTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ConfirmDialogContentPreview() {
    ApkAnalyzerTvTheme {
        ConfirmDialogContent(
            title = "Allow usage access",
            text = "Sorting by last used needs to know when you last opened each app.",
            confirmLabel = "Open settings",
            dismissLabel = "Not now",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
