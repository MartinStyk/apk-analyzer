package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    initialStartMs: Long? = null,
    initialEndMs: Long? = null,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMs,
        initialSelectedEndDateMillis = initialEndMs,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        onConfirm(start, end)
                    }
                    onDismiss()
                },
            ) {
                Text(
                    text = stringResource(android.R.string.ok),
                    color = AppTheme.colors.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
        },
        modifier = modifier,
    ) {
        DateRangePicker(state = state)
    }
}

@Preview
@Composable
private fun DateRangePickerDialogPreview() {
    ApkAnalyzerTheme {
        DateRangePickerDialog(
            onDismiss = {},
            onConfirm = { _, _ -> },
            initialStartMs = 1_736_640_000_000,
            initialEndMs = 1_748_736_000_000,
        )
    }
}
