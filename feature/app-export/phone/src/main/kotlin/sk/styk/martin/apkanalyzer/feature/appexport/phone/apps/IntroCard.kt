package sk.styk.martin.apkanalyzer.feature.appexport.phone.apps

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.InfoCard
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appexport.phone.R

@Composable
internal fun IntroCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    InfoCard(
        icon = ApkAnalyzerIcons.Export,
        title = stringResource(R.string.inventory_export_intro_title),
        body = stringResource(R.string.inventory_export_intro_description),
        modifier = modifier,
        action = {
            TextButton(
                text = stringResource(R.string.inventory_export_intro_got_it),
                onClick = onDismiss,
            )
        },
    )
}

@Preview
@Composable
private fun IntroCardPreview() {
    ApkAnalyzerTheme {
        IntroCard(onDismiss = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview
@Composable
private fun IntroCardDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        IntroCard(onDismiss = {}, modifier = Modifier.padding(16.dp))
    }
}
