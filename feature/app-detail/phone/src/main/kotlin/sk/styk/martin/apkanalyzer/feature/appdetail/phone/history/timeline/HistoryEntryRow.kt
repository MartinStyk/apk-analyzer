package sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.timeline

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistorySummaryPart
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.AppHistoryVersion
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.ValueChange
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.breakHeadlineRes
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.collapsedToTwoLines
import sk.styk.martin.apkanalyzer.core.apphistory.timeline.model.label
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Tag
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.shape
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.R
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.HistoryVersionTag
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.historyDate
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.historyDateTime
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.historyFullDate
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.text
import sk.styk.martin.apkanalyzer.feature.appdetail.phone.history.unlessSameAs
import java.time.Instant
import sk.styk.martin.apkanalyzer.core.apphistory.R as HistoryR

@Composable
internal fun HistoryEntryRow(
    row: AppHistoryRow,
    position: ListItemPosition,
    onClick: () -> Unit,
    onLongClick: (copyText: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val onRowColor = if (row.hasCertificateChange) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant
    val headline = row.headline(resources)
    val versionLine = row.versionText(resources).unlessSameAs(headline)
    val date = remember(row.date, row.showsTime) { if (row.showsTime) row.date.historyDateTime() else row.date.historyDate() }
    Row(
        verticalAlignment = Alignment.Top,
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
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = row.icon,
            contentDescription = null,
            tint = row.iconTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = date,
                style = AppTheme.typography.labelMedium,
                color = onRowColor,
                maxLines = 1,
            )
            Text(
                text = headline,
                style = AppTheme.typography.titleSmall,
                color = when {
                    row.hasCertificateChange -> AppTheme.colors.onSurface
                    row.isQuiet -> AppTheme.colors.onSurfaceVariant
                    else -> AppTheme.colors.onBackground
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.hasCertificateChange) {
                row.breakHeadlineRes?.let { BodyLine(text = stringResource(it), color = onRowColor) }
            }
            versionLine?.let { BodyLine(text = it, color = onRowColor) }
            row.summaryLines(resources).forEach { BodyLine(text = it, color = onRowColor) }
            row.versionTag?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Tag(text = stringResource(it.labelRes))
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = ApkAnalyzerIcons.ChevronRight,
            contentDescription = null,
            tint = onRowColor,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.CenterVertically),
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

private val AppHistoryRow.breakHeadlineRes: Int?
    get() = changeKind?.breakHeadlineRes

private val AppHistoryRow.icon: ImageVector
    get() = when {
        hasCertificateChange -> ApkAnalyzerIcons.Warning

        else -> when (changeKind) {
            null -> ApkAnalyzerIcons.History
            AppHistoryEntry.Change.Kind.Updated -> ApkAnalyzerIcons.Sync
            AppHistoryEntry.Change.Kind.Reinstalled -> ApkAnalyzerIcons.Export
            AppHistoryEntry.Change.Kind.NewDevice -> ApkAnalyzerIcons.Screen
        }
    }

private val AppHistoryRow.iconTint: Color
    @Composable
    get() = when {
        hasCertificateChange -> AppTheme.colors.warning
        isQuiet || kind == AppHistoryRowKind.FirstRecorded || changeKind == AppHistoryEntry.Change.Kind.NewDevice -> AppTheme.colors.onSurfaceVariant
        else -> AppTheme.colors.primary
    }

private fun AppHistoryRow.versionText(resources: Resources): String = version.text(versionChange, resources)

private fun AppHistoryRow.headline(resources: Resources): String = when {
    hasCertificateChange -> resources.getString(HistoryR.string.app_history_certificate_changed)
    kind == AppHistoryRowKind.FirstRecorded -> resources.getString(HistoryR.string.app_history_first_recorded)
    else -> breakHeadlineRes?.let(resources::getString) ?: versionText(resources)
}

private fun AppHistoryRow.summaryLines(resources: Resources): List<String> {
    if (hasOnlyUncomparableDetails) return listOf(resources.getString(HistoryR.string.app_history_uncomparable_only))
    return summary.map { it.label(resources) }.collapsedToTwoLines(resources)
}

private fun AppHistoryRow.copyText(resources: Resources): String {
    val headlineOrKind = if (changeKind == AppHistoryEntry.Change.Kind.Updated && !hasCertificateChange) null else headline(resources)
    return (listOfNotNull(headlineOrKind, date.historyFullDate(), versionText(resources)) + summary.map { it.label(resources) })
        .reduce { joined, part -> resources.getString(HistoryR.string.app_history_summary_join, joined, part) }
}

private val sampleDate = Instant.ofEpochMilli(1_772_409_600_000)

internal fun sampleHistoryRow(
    kind: AppHistoryRowKind = AppHistoryRowKind.Change(AppHistoryEntry.Change.Kind.Updated),
    snapshotId: Long = 4,
    versionChange: ValueChange<AppHistoryVersion>? = ValueChange(AppHistoryVersion(31200, "312.0"), AppHistoryVersion(31500, "315.0")),
    summary: List<AppHistorySummaryPart> = listOf(
        AppHistorySummaryPart.PermissionsAdded(firstLabel = "Camera", otherCount = 1),
        AppHistorySummaryPart.ComponentsAdded(count = 3),
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
