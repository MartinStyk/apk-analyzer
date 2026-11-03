package sk.styk.martin.apkanalyzer.feature.appexport.phone.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportStep

@Composable
internal fun StepGuidance(step: InventoryExportStep, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(step.guidanceRes()),
        style = AppTheme.typography.bodyMedium,
        color = AppTheme.colors.onSurfaceVariant,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StepGuidancePreview() {
    ApkAnalyzerTheme {
        StepGuidance(step = InventoryExportStep.Columns)
    }
}
