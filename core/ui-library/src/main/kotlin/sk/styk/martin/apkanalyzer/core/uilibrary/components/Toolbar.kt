package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun Toolbar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
            .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        IconButton(
            imageVector = ApkAnalyzerIcons.Back,
            onClick = onBack,
        )
        Text(
            text = title,
            style = AppTheme.typography.titleLarge,
            color = AppTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Preview
@Composable
private fun ToolbarSimplePreview() {
    ApkAnalyzerTheme {
        Toolbar(
            title = "Filter",
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ToolbarWithActionPreview() {
    ApkAnalyzerTheme {
        Toolbar(
            title = "Filter",
            onBack = {},
            actions = {
                TextButton(text = "Reset", onClick = {})
            },
        )
    }
}
