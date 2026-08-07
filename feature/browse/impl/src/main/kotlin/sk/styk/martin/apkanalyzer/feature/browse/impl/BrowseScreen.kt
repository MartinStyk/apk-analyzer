package sk.styk.martin.apkanalyzer.feature.browse.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme

@Composable
internal fun BrowseScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Browse")
    }
}

@Preview
@Composable
private fun BrowseScreenPreview() {
    ApkAnalyzerTheme {
        BrowseScreen()
    }
}
