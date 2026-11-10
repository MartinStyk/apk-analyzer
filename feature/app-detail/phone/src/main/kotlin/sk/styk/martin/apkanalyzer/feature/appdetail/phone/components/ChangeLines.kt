package sk.styk.martin.apkanalyzer.feature.appdetail.phone.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R

@Composable
internal fun SheetSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Composable
internal fun ChangeLinesSection(lines: List<String>, modifier: Modifier = Modifier) {
    if (lines.isEmpty()) return
    SheetSection(title = stringResource(R.string.app_history_what_changed), modifier = modifier) {
        lines.forEach { line ->
            Text(
                text = line,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurface,
            )
        }
    }
}

@Composable
internal fun ColumnScope.RowChangeLines(lines: List<String>) {
    lines.forEach { line ->
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = line,
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurface,
        )
    }
}

@Preview
@Composable
private fun ChangeLinesSectionPreview() {
    ApkAnalyzerTheme {
        Column {
            ChangeLinesSection(lines = listOf("Exported: No → Yes", "Permission guard: None → android.permission.BIND_JOB_SERVICE"))
            RowChangeLines(lines = listOf("Protection level: Normal → Dangerous"))
        }
    }
}
