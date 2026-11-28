package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.R
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    ProBadgeLabel(containerColor = AppTheme.colors.proContainer, modifier = modifier)
}

@Composable
fun ProBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(Shapes.CardShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        ProBadgeLabel(containerColor = Color.Transparent)
    }
}

@Composable
private fun ProBadgeLabel(containerColor: Color, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.content_description_pro)
    Tag(
        text = stringResource(R.string.pro_badge),
        icon = ApkAnalyzerIcons.Star,
        contentColor = AppTheme.colors.pro,
        containerColor = containerColor,
        textStyle = AppTheme.typography.labelMedium,
        modifier = modifier
            .heightIn(min = 24.dp)
            .clearAndSetSemantics { contentDescription = description },
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

@Preview(fontScale = 2f)
@Composable
private fun ProBadgeClickableLargeTextPreview() {
    ApkAnalyzerTheme {
        ProBadge(onClick = {})
    }
}
