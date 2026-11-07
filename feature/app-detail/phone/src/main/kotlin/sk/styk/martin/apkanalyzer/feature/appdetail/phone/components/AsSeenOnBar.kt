package sk.styk.martin.apkanalyzer.feature.appdetail.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.historyFullDate
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.recordedAt
import java.time.Instant

@Composable
internal fun AppDetailModeFrame(
    input: AppDetailInput,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        input.recordedAt?.let { AsSeenOnBar(recordedAt = it) }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
internal fun AsSeenOnBar(recordedAt: Instant, modifier: Modifier = Modifier) {
    var showExplanation by rememberSaveable { mutableStateOf(false) }
    val date = recordedAt.historyFullDate()
    val text = stringResource(R.string.app_history_as_seen_on, date)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(AppTheme.colors.secondaryContainer)
            .semantics(mergeDescendants = true) { paneTitle = text }
            .clickable(
                onClickLabel = stringResource(R.string.app_history_as_seen_click_label),
                role = Role.Button,
                onClick = { showExplanation = true },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.History,
            contentDescription = null,
            tint = AppTheme.colors.onSecondaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = AppTheme.typography.labelLarge,
            color = AppTheme.colors.onSecondaryContainer,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = ApkAnalyzerIcons.Info,
            contentDescription = null,
            tint = AppTheme.colors.onSecondaryContainer,
            modifier = Modifier.size(18.dp),
        )
    }
    if (showExplanation) {
        RationaleBottomSheet(
            row = InfoRow(
                label = stringResource(R.string.app_history_as_seen_on_label),
                value = date,
                rationale = stringResource(R.string.app_history_as_seen_on_explanation),
            ),
            onDismiss = { showExplanation = false },
        )
    }
}

@Preview
@Composable
private fun AsSeenOnBarPreview() {
    ApkAnalyzerTheme {
        AsSeenOnBar(recordedAt = Instant.ofEpochMilli(1_773_014_400_000))
    }
}

@Preview
@Composable
private fun AsSeenOnBarDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        AsSeenOnBar(recordedAt = Instant.ofEpochMilli(1_773_014_400_000))
    }
}
