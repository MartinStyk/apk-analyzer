package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.timeline

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryChangeDirection
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.breakHeadlineRes
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.direction
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.label
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.needsAttention
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.shape
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.components.ChangeMarkerDot
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.HistoryVersionTag
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.historyDate
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.historyDateTime
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.historyFullDate
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.icon
import java.time.Instant
import sk.styk.martin.apkanalyzer.core.apphistory.api.R as HistoryR

private const val MAX_VISIBLE_TOKENS = 5

@Composable
internal fun HistoryEntryRow(
    row: AppHistoryRow,
    position: ListItemPosition,
    onClick: () -> Unit,
    onLongClick: (copyText: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val secondaryColor = if (row.hasCertificateChange) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant
    val date = remember(row.date, row.showsTime) { if (row.showsTime) row.date.historyDateTime() else row.date.historyDate() }
    Row(
        verticalAlignment = if (row.isCompact) Alignment.CenterVertically else Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .background(if (row.hasCertificateChange) AppTheme.colors.warningContainer else AppTheme.colors.surface, position.shape())
            .padding(2.dp)
            .clip(Shapes.CardShape)
            .combinedClickable(
                onClickLabel = stringResource(
                    if (row.kind == AppHistoryRowKind.FirstRecorded) R.string.app_history_row_first_click_label else R.string.app_history_row_click_label,
                ),
                onLongClickLabel = stringResource(R.string.app_history_row_long_click_label),
                onClick = onClick,
                onLongClick = { onLongClick(row.copyText(resources)) },
            )
            .padding(horizontal = 12.dp, vertical = if (row.isCompact) 8.dp else 12.dp),
    ) {
        Icon(
            imageVector = row.icon,
            contentDescription = null,
            tint = row.iconTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        if (row.isCompact) {
            CompactRowContent(row = row, date = date, modifier = Modifier.weight(1f))
        } else {
            FullRowContent(row = row, date = date, secondaryColor = secondaryColor, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = ApkAnalyzerIcons.ChevronRight,
            contentDescription = null,
            tint = secondaryColor,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.CenterVertically),
        )
    }
}

@Composable
private fun CompactRowContent(
    row: AppHistoryRow,
    date: String,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    HeadlineWithDate(
        headline = row.compactLine(row.versionText(resources), resources),
        headlineStyle = AppTheme.typography.bodySmall,
        headlineColor = AppTheme.colors.onSurfaceVariant,
        date = date,
        dateColor = AppTheme.colors.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun FullRowContent(
    row: AppHistoryRow,
    date: String,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        HeadlineWithDate(
            headline = row.versionText(LocalResources.current),
            headlineStyle = AppTheme.typography.titleSmall,
            headlineColor = AppTheme.colors.onBackground,
            date = date,
            dateColor = secondaryColor,
        )
        if (row.hasCertificateChange) {
            Text(
                text = stringResource(R.string.app_history_certificate_changed),
                style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = AppTheme.colors.onSurface,
            )
        }
        row.breakHeadlineRes?.let { BodyLine(text = stringResource(it), color = secondaryColor) }
        if (row.hasOnlyUncomparableDetails) {
            BodyLine(text = stringResource(R.string.app_history_uncomparable_only), color = secondaryColor)
        } else {
            ChangeTokens(parts = row.summary, textColor = secondaryColor)
        }
    }
}

@Composable
private fun HeadlineWithDate(
    headline: String,
    headlineStyle: TextStyle,
    headlineColor: Color,
    date: String,
    dateColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = headline,
            style = headlineStyle,
            color = headlineColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = date,
            style = AppTheme.typography.labelMedium,
            color = dateColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun ChangeTokens(parts: List<AppHistorySummaryPart>, textColor: Color) {
    if (parts.isEmpty()) return
    val resources = LocalResources.current
    val tokens = remember(parts, resources) {
        parts.sortedByDescending { it.needsAttention }.map { part -> part to part.label(resources) }
    }
    val visible = tokens.take(MAX_VISIBLE_TOKENS)
    val hiddenCount = tokens.size - visible.size
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        visible.forEach { (part, label) ->
            ChangeToken(text = label, marker = part.direction, needsAttention = part.needsAttention, textColor = textColor)
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

@Composable
private fun ChangeToken(
    text: String,
    marker: AppHistoryChangeDirection,
    needsAttention: Boolean,
    textColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (needsAttention) {
            ChangeMarkerDot(marker = marker, contentDescription = null, color = AppTheme.colors.tertiary)
        } else {
            ChangeMarkerDot(marker = marker, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = if (needsAttention) AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium) else AppTheme.typography.bodySmall,
            color = if (needsAttention) AppTheme.colors.tertiary else textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BodyLine(text: String, color: Color) {
    Text(
        text = text,
        style = AppTheme.typography.bodySmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private val AppHistoryRow.isCompact: Boolean
    get() = isQuiet || kind == AppHistoryRowKind.FirstRecorded

private val AppHistoryRow.breakHeadlineRes: Int?
    get() = changeKind?.breakHeadlineRes

private val AppHistoryRow.icon: ImageVector
    get() = when {
        hasCertificateChange -> ApkAnalyzerIcons.Warning
        else -> changeKind?.icon(versionTag) ?: ApkAnalyzerIcons.History
    }

private val AppHistoryRow.iconTint: Color
    @Composable
    get() = when {
        hasCertificateChange -> AppTheme.colors.warning
        isCompact || changeKind == AppHistoryEntry.Change.Kind.NewDevice -> AppTheme.colors.onSurfaceVariant
        else -> AppTheme.colors.primary
    }

private fun AppHistoryRow.versionText(resources: Resources): String =
    versionChange?.let { resources.getString(HistoryR.string.app_history_version, it.label(resources)) } ?: version.label(resources)

private fun AppHistoryRow.compactLine(versionText: String, resources: Resources): String {
    val detail = if (kind == AppHistoryRowKind.FirstRecorded) {
        resources.getString(R.string.app_history_first_recorded)
    } else {
        summary.joinedLabels(resources)
    }
    return detail?.let { resources.getString(HistoryR.string.app_history_summary_join, versionText, it) } ?: versionText
}

private fun List<AppHistorySummaryPart>.joinedLabels(resources: Resources): String? = map { it.label(resources) }
    .reduceOrNull { joined, part -> resources.getString(HistoryR.string.app_history_summary_join, joined, part) }

private fun AppHistoryRow.copyText(resources: Resources): String {
    val event = when {
        hasCertificateChange -> resources.getString(R.string.app_history_certificate_changed)
        kind == AppHistoryRowKind.FirstRecorded -> resources.getString(R.string.app_history_first_recorded)
        else -> breakHeadlineRes?.let(resources::getString)
    }
    return (listOfNotNull(event, date.historyFullDate(), versionText(resources)) + summary.map { it.label(resources) })
        .reduce { joined, part -> resources.getString(HistoryR.string.app_history_summary_join, joined, part) }
}

private val sampleDate = Instant.ofEpochMilli(1_772_409_600_000)

internal fun sampleHistoryRow(
    kind: AppHistoryRowKind = AppHistoryRowKind.Change(AppHistoryEntry.Change.Kind.Updated),
    snapshotId: Long = 4,
    versionChange: ValueChange<AppHistoryVersion>? = ValueChange(AppHistoryVersion(31200, "312.0"), AppHistoryVersion(31500, "315.0")),
    summary: List<AppHistorySummaryPart> = listOf(
        AppHistorySummaryPart.DangerousPermissionsAdded(firstLabel = "Camera", otherCount = 0),
        AppHistorySummaryPart.PermissionsAdded(firstLabel = "View Wi-Fi connections", otherCount = 0),
        AppHistorySummaryPart.ComponentsAdded(count = 6),
        AppHistorySummaryPart.NativeLibrariesRemoved(count = 1),
        AppHistorySummaryPart.TargetSdkChanged(before = 34, after = 35),
        AppHistorySummaryPart.SizeChanged(before = 152.megabytes, after = 160.megabytes),
    ),
    hasCertificateChange: Boolean = false,
    isQuiet: Boolean = false,
    versionTag: HistoryVersionTag? = null,
    hasOnlyUncomparableDetails: Boolean = false,
) = AppHistoryRow(
    snapshotId = snapshotId,
    capturedAt = sampleDate,
    date = sampleDate,
    kind = kind,
    version = AppHistoryVersion(31500, "315.0"),
    versionChange = versionChange,
    summary = summary,
    hasCertificateChange = hasCertificateChange,
    isQuiet = isQuiet,
    versionTag = versionTag,
    hasOnlyUncomparableDetails = hasOnlyUncomparableDetails,
)

@Preview
@Composable
private fun HistoryEntryRowNormalPreview() {
    ApkAnalyzerTheme {
        HistoryEntryRow(row = sampleHistoryRow(), position = ListItemPosition.Single, onClick = {}, onLongClick = {})
    }
}

@Preview
@Composable
private fun HistoryEntryRowQuietPreview() {
    ApkAnalyzerTheme {
        HistoryEntryRow(
            row = sampleHistoryRow(summary = listOf(AppHistorySummaryPart.SizeChanged(before = 150.megabytes, after = 152.megabytes)), isQuiet = true),
            position = ListItemPosition.Single,
            onClick = {},
            onLongClick = {},
        )
    }
}

@Preview
@Composable
private fun HistoryEntryRowCertificateOnReinstallPreview() {
    ApkAnalyzerTheme {
        HistoryEntryRow(
            row = sampleHistoryRow(kind = AppHistoryRowKind.Change(AppHistoryEntry.Change.Kind.Reinstalled), hasCertificateChange = true),
            position = ListItemPosition.Single,
            onClick = {},
            onLongClick = {},
        )
    }
}

@Preview
@Composable
private fun HistoryEntryRowUncomparableOnlyPreview() {
    ApkAnalyzerTheme {
        HistoryEntryRow(
            row = sampleHistoryRow(
                versionChange = null,
                summary = emptyList(),
                versionTag = HistoryVersionTag.SameVersionBuild,
                hasOnlyUncomparableDetails = true,
            ),
            position = ListItemPosition.Single,
            onClick = {},
            onLongClick = {},
        )
    }
}
