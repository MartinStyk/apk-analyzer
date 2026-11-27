package sk.styk.martin.apkanalyzer.feature.appdetail.phone.requirements

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.DeviceFeatures
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.FeatureAvailability
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.RequirementItem
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.label
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.missingText
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.toRequirementItem
import sk.styk.martin.apkanalyzer.core.common.model.ChangeDirection
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.ChangeMarkedIcon
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.RowChangeLines
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.markedBy

@Composable
internal fun RequirementRow(
    item: RequirementItem,
    onClick: () -> Unit,
    onLongClick: (label: String) -> Unit,
    modifier: Modifier = Modifier,
    changeLines: List<String> = emptyList(),
    changeMarker: ChangeDirection? = null,
) {
    val label = item.label(LocalResources.current)
    val showIdentifier = label != item.identifier

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(Shapes.CardShape)
            .background(AppTheme.colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = { onLongClick(label) })
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        ChangeMarkedIcon(
            imageVector = item.icon,
            contentDescription = null,
            tint = AppTheme.colors.onSurfaceVariant,
            marker = changeMarker,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = AppTheme.typography.titleSmall.markedBy(changeMarker),
                color = AppTheme.colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showIdentifier) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.identifier,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
            RowChangeLines(lines = changeLines)
        }
        if (item.availability == FeatureAvailability.Missing) {
            Spacer(modifier = Modifier.width(8.dp))
            MissingMarker(item = item)
        }
    }
}

@Composable
private fun MissingMarker(item: RequirementItem, modifier: Modifier = Modifier) {
    Text(
        text = item.missingText(LocalResources.current),
        style = AppTheme.typography.labelMedium,
        color = if (item.isRequired) AppTheme.colors.warning else AppTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = modifier.width(96.dp),
    )
}

@Preview
@Composable
private fun RequirementRowPreview() {
    ApkAnalyzerTheme {
        RequirementRow(
            item = Feature.Hardware(name = "android.hardware.camera", isRequired = true).toRequirementItem(DeviceFeatures.Unknown),
            onClick = {},
            onLongClick = {},
        )
    }
}

@Preview
@Composable
private fun RequirementRowAddedPreview() {
    ApkAnalyzerTheme {
        RequirementRow(
            item = Feature.Hardware(name = "android.hardware.nfc").toRequirementItem(DeviceFeatures.Unknown),
            onClick = {},
            onLongClick = {},
            changeMarker = ChangeDirection.Added,
        )
    }
}
