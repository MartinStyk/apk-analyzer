package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

@Composable
fun ProgressBar(progress: () -> Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = AppTheme.colors.primary,
        trackColor = AppTheme.colors.primaryContainer,
        strokeCap = StrokeCap.Round,
    )
}

@Preview
@Composable
private fun ProgressBarPreview() {
    ApkAnalyzerTheme {
        ProgressBar(progress = { 0.4f }, modifier = Modifier.fillMaxWidth())
    }
}
