package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
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

@Preview
@Composable
private fun SwitchDisabledDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        Switch(checked = false, onCheckedChange = {}, enabled = false)
    }
}
