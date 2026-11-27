package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.phone.R
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.content_description_pro)
    Tag(
        text = stringResource(R.string.pro_badge),
        icon = ApkAnalyzerIcons.Lock,
        highlighted = true,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    )
}

@Preview
@Composable
private fun ProBadgePreview() {
    ApkAnalyzerTheme {
        ProBadge()
    }
}
