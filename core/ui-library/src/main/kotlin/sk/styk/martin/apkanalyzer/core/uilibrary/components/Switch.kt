package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedTrackColor = AppTheme.colors.primary,
            checkedThumbColor = AppTheme.colors.onPrimary,
            uncheckedTrackColor = AppTheme.colors.surfaceVariant,
            uncheckedThumbColor = AppTheme.colors.onSurfaceVariant,
        ),
    )
}

@Preview
@Composable
private fun SwitchOnPreview() {
    ApkAnalyzerTheme {
        Switch(checked = true, onCheckedChange = {})
    }
}

@Preview
@Composable
private fun SwitchOffPreview() {
    ApkAnalyzerTheme {
        Switch(checked = false, onCheckedChange = {})
    }
}
