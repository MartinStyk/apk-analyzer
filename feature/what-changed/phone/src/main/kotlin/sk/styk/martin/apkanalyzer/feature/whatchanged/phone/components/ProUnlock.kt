package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ProButton
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ProMark
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.R

private val BarShape = RoundedCornerShape(20.dp)

@Composable
internal fun ProUnlockCard(
    lockedAppCount: Int,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.proContainer, Shapes.CardShape)
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
    ) {
        ProMark(size = 22.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = pluralStringResource(R.plurals.what_changed_pro_title, lockedAppCount, lockedAppCount),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
            )
            Text(
                text = stringResource(R.string.what_changed_pro_card_body),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        ProButton(text = stringResource(R.string.what_changed_pro_unlock), onClick = onUnlock)
    }
}

@Composable
internal fun ProUnlockBar(
    lockedAppCount: Int,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = BarShape)
            .background(AppTheme.colors.proContainer, BarShape)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
    ) {
        ProMark(size = 22.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pluralStringResource(R.plurals.what_changed_pro_title, lockedAppCount, lockedAppCount),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
            )
            Text(
                text = stringResource(R.string.what_changed_pro_bar_body),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        ProButton(text = stringResource(R.string.what_changed_pro_unlock), onClick = onUnlock)
    }
}

@Preview
@Composable
private fun ProUnlockCardPreview() {
    ApkAnalyzerTheme {
        ProUnlockCard(lockedAppCount = 55, onUnlock = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview
@Composable
private fun ProUnlockBarPreview() {
    ApkAnalyzerTheme {
        ProUnlockBar(lockedAppCount = 55, onUnlock = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview
@Composable
private fun ProUnlockBarDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        ProUnlockBar(lockedAppCount = 55, onUnlock = {}, modifier = Modifier.padding(16.dp))
    }
}
