package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.R
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.content_description_pro)
    Tag(
        text = stringResource(R.string.pro_badge),
        icon = ApkAnalyzerIcons.Lock,
        contentColor = AppTheme.colors.pro,
        containerColor = AppTheme.colors.proContainer,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    )
}

@Preview(showBackground = true)
@Composable
private fun ProBadgePreview() {
    ApkAnalyzerTheme(isDarkTheme = false) {
        ProBadge()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF161618)
@Composable
private fun ProBadgeDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        ProBadge()
    }
}
