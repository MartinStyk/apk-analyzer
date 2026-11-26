package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.change

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.card
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.InfoRow
import sk.styk.martin.apkanalyzer.core.apphistory.api.R as HistoryR

@Composable
internal fun ChangedValueRow(
    label: String,
    change: ValueChange<String>,
    rationale: String,
    onShowRationale: (InfoRow) -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
    diff: String? = null,
) {
    val fullValue = stringResource(HistoryR.string.app_history_value_change, change.before, change.after)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .clip(Shapes.CardShape)
            .combinedClickable(
                onClick = { onShowRationale(InfoRow(label, fullValue, rationale)) },
                onLongClick = { onCopy(label, fullValue) },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
            diff?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = fullValue },
        ) {
            Text(
                text = change.before,
                style = AppTheme.typography.titleSmall.copy(textDecoration = TextDecoration.LineThrough),
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
            Text(
                text = change.after,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Preview
@Composable
private fun ChangedValueRowPreview() {
    ApkAnalyzerTheme {
        ChangedValueRow(
            label = "APK size",
            change = ValueChange("152 MB", "160 MB"),
            diff = "+8 MB",
            rationale = "How much space the app's installation files take on this device.",
            onShowRationale = {},
            onCopy = { _, _ -> },
            modifier = Modifier.card(),
        )
    }
}

@Preview
@Composable
private fun ChangedValueRowLongValuesPreview() {
    ApkAnalyzerTheme {
        ChangedValueRow(
            label = "Target SDK",
            change = ValueChange("34 (Android 14)", "35 (Android 15)"),
            rationale = "The Android version the app was built and tested for.",
            onShowRationale = {},
            onCopy = { _, _ -> },
            modifier = Modifier.card(),
        )
    }
}
