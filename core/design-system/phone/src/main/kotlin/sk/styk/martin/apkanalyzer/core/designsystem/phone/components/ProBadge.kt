package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

@Composable
fun ProBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.content_description_pro)
    Chip(
        label = stringResource(R.string.pro_badge),
        variant = ChipVariant.Tonal,
        leadingIcon = ApkAnalyzerIcons.Lock,
        onClick = onClick,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = description },
    )
}

@Preview
@Composable
private fun ProBadgePreview() {
    ApkAnalyzerTheme {
        ProBadge()
    }
}

@Preview
@Composable
private fun ProBadgeClickablePreview() {
    ApkAnalyzerTheme {
        ProBadge(onClick = {})
    }
}

@Preview
@Composable
private fun ProBadgeClickableDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        ProBadge(onClick = {})
    }
}
