package sk.styk.martin.apkanalyzer.feature.appexport.phone.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.TextButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appexport.phone.R

@Composable
internal fun IntroCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.secondaryContainer, Shapes.CardShape)
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = ApkAnalyzerIcons.Export,
                tint = AppTheme.colors.onSecondaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.inventory_export_intro_title),
                    style = AppTheme.typography.titleSmall,
                    color = AppTheme.colors.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.inventory_export_intro_description),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSecondaryContainer,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                text = stringResource(R.string.inventory_export_intro_got_it),
                onClick = onDismiss,
            )
        }
    }
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
