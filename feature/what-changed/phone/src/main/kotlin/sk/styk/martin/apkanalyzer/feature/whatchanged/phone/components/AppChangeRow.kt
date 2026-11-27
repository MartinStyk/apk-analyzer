package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.components

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.breakHeadlineRes
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.direction
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.label
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.needsAttention
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.AppIcon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.ChangeToken
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.format.historyDate
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.shape
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.R
import java.time.Instant
import sk.styk.martin.apkanalyzer.core.apphistory.api.R as HistoryR

private const val MAX_VISIBLE_TOKENS = 5
private val AppIconSize = 36.dp

@Composable
internal fun AppChangeRow(
    item: AppChangeItem,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: ((copyText: String) -> Unit)? = null,
) {
    val resources = LocalResources.current
    val secondaryColor = if (item.hasCertificateChange) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant
    RowFrame(
        packageName = item.packageName,
        isWarning = item.hasCertificateChange,
        position = position,
        clickLabel = stringResource(
            if (item.kind ==
                AppChangeKind.FirstRecorded
            ) {
                R.string.what_changed_row_first_click_label
            } else {
                R.string.what_changed_row_click_label
            },
        ),
        onClick = onClick,
        onLongClick = onLongClick?.let { copy -> { copy(item.copyText(resources)) } },
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NameWithDate(name = item.appName, date = item.date.historyDate(), dateColor = secondaryColor)
            Text(
                text = item.versionLine(resources),
                style = AppTheme.typography.bodySmall,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.hasCertificateChange) {
                Text(
                    text = stringResource(R.string.what_changed_certificate_changed),
                    style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.onSurface,
                )
            }
            (item.kind as? AppChangeKind.Change)?.kind?.breakHeadlineRes?.let {
                Text(text = stringResource(it), style = AppTheme.typography.bodySmall, color = secondaryColor)
            }
            if (item.hasOnlyUncomparableDetails) {
                Text(text = stringResource(R.string.what_changed_uncomparable_only), style = AppTheme.typography.bodySmall, color = secondaryColor)
            } else {
                ChangeTokens(parts = item.parts, textColor = secondaryColor)
            }
            item.revertedAt?.let {
                Text(
                    text = stringResource(R.string.what_changed_reverted, it.historyDate()),
                    style = AppTheme.typography.bodySmall,
                    color = secondaryColor,
                )
            }
        }
    }
}

@Composable
internal fun AppHistoryAppRow(
    packageName: String,
    appName: String,
    supportingText: String,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    parts: List<AppHistorySummaryPart> = emptyList(),
    isWarning: Boolean = false,
) {
    val secondaryColor = if (isWarning) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant
    RowFrame(
        packageName = packageName,
        isWarning = isWarning,
        position = position,
        clickLabel = stringResource(R.string.what_changed_app_click_label),
        onClick = onClick,
        onLongClick = null,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = appName,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = supportingText, style = AppTheme.typography.bodySmall, color = secondaryColor)
            ChangeTokens(parts = parts, textColor = secondaryColor)
        }
    }
}

@Composable
private fun RowFrame(
    packageName: String,
    isWarning: Boolean,
    position: ListItemPosition,
    clickLabel: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .background(if (isWarning) AppTheme.colors.warningContainer else AppTheme.colors.surface, position.shape())
            .padding(2.dp)
            .clip(Shapes.CardShape)
            .combinedClickable(
                onClickLabel = clickLabel,
                onLongClickLabel = onLongClick?.let { stringResource(R.string.what_changed_row_long_click_label) },
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(12.dp),
    ) {
        AppIcon(
            source = AppReference.InstalledPackage(PackageName(packageName)),
            size = AppIconSize,
            missingIcon = ApkAnalyzerIcons.Android,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) { content() }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = ApkAnalyzerIcons.ChevronRight,
            contentDescription = null,
            tint = if (isWarning) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.CenterVertically),
        )
    }
}

@Composable
private fun NameWithDate(
    name: String,
    date: String,
    dateColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = date, style = AppTheme.typography.labelMedium, color = dateColor, maxLines = 1)
    }
}

@Composable
private fun ChangeTokens(parts: List<AppHistorySummaryPart>, textColor: Color) {
    if (parts.isEmpty()) return
    val resources = LocalResources.current
    val visible = parts.take(MAX_VISIBLE_TOKENS)
    val hiddenCount = parts.size - visible.size
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        visible.forEach { part ->
            ChangeToken(text = part.label(resources), direction = part.direction, isEmphasized = part.needsAttention, textColor = textColor)
        }
        if (hiddenCount > 0) {
            Text(
                text = resources.getQuantityString(HistoryR.plurals.app_history_summary_more, hiddenCount, hiddenCount),
                style = AppTheme.typography.bodySmall,
                color = textColor,
            )
        }
    }
}

private fun AppChangeItem.versionLine(resources: Resources): String = when (kind) {
    AppChangeKind.FirstRecorded -> resources.getString(
        HistoryR.string.app_history_summary_join,
        resources.getString(R.string.what_changed_first_recorded),
        version.label(resources),
    )

    is AppChangeKind.Change -> versionChange?.label(resources) ?: version.label(resources)
}

private fun AppChangeItem.copyText(resources: Resources): String = buildList {
    add(appName)
    add(date.historyDate())
    add(versionLine(resources))
    if (hasCertificateChange) add(resources.getString(R.string.what_changed_certificate_changed))
    addAll(parts.map { it.label(resources) })
}.joinToString(separator = "\n")

private val PreviewChange = AppChangeItem(
    packageName = "com.instagram.android",
    appName = "Instagram",
    snapshotId = 2,
    capturedAt = Instant.parse("2026-11-26T10:00:00Z"),
    date = Instant.parse("2026-11-26T09:00:00Z"),
    kind = AppChangeKind.Change(AppHistoryEntry.Change.Kind.Updated),
    version = AppHistoryVersion(315, "315.0"),
    versionChange = ValueChange(AppHistoryVersion(312, "312.0"), AppHistoryVersion(315, "315.0")),
    parts = listOf(
        AppHistorySummaryPart.DangerousPermissionsAdded(firstLabel = "Camera", otherCount = 0),
        AppHistorySummaryPart.ComponentsAdded(2),
        AppHistorySummaryPart.SizeChanged(120.megabytes, 128.megabytes),
    ),
    needsAttention = true,
)

@Preview
@Composable
private fun AppChangeRowPreview() {
    ApkAnalyzerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AppChangeRow(item = PreviewChange, position = ListItemPosition.First, onClick = {})
            AppChangeRow(
                item = PreviewChange.copy(
                    packageName = "com.whatsapp",
                    appName = "WhatsApp",
                    parts = emptyList(),
                    hasCertificateChange = true,
                    revertedAt = null,
                ),
                position = ListItemPosition.Middle,
                onClick = {},
            )
            AppChangeRow(
                item = PreviewChange.copy(
                    packageName = "org.thoughtcrime.securesms",
                    appName = "Signal",
                    kind = AppChangeKind.FirstRecorded,
                    parts = emptyList(),
                ),
                position = ListItemPosition.Last,
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun AppHistoryAppRowPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            AppHistoryAppRow(
                packageName = "com.instagram.android",
                appName = "Instagram",
                supportingText = "2 updates",
                parts = PreviewChange.parts,
                position = ListItemPosition.First,
                onClick = {},
            )
            AppHistoryAppRow(
                packageName = "com.torch.pro",
                appName = "Torch Pro",
                supportingText = "5 changes",
                position = ListItemPosition.Last,
                onClick = {},
            )
        }
    }
}
