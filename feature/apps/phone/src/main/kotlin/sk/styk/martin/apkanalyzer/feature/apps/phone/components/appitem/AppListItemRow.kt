package sk.styk.martin.apkanalyzer.feature.apps.phone.components.appitem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.apps.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.AppRow
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.feature.apps.phone.R
import sk.styk.martin.apkanalyzer.feature.apps.phone.list.AppListItem
import java.text.DateFormat
import java.time.Instant
import java.util.Date

@Composable
internal fun AppListItemRow(
    app: AppListItem,
    onClick: () -> Unit,
    position: ListItemPosition,
    modifier: Modifier = Modifier,
    sortType: AppSortType = AppSortType.Name,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val showsSortValue = sortType != AppSortType.Name
    AppRow(
        packageName = app.packageName,
        applicationName = app.applicationName,
        onClick = onClick,
        position = position,
        modifier = modifier,
        trailingContent = if (showsSortValue || trailingContent != null) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (showsSortValue) SortValueBadge(app = app, sortType = sortType)
                    trailingContent?.invoke()
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun SortValueBadge(
    app: AppListItem,
    sortType: AppSortType,
    modifier: Modifier = Modifier,
) {
    val (icon, value) = when (sortType) {
        AppSortType.ApkSize -> ApkAnalyzerIcons.FileUpload to app.apkSize.formatted()
        AppSortType.TotalSize -> ApkAnalyzerIcons.Storage to (app.totalSize?.formatted() ?: "—")
        AppSortType.InstallDate -> ApkAnalyzerIcons.Calendar to app.installTime.toShortDate(stringResource(R.string.preinstalled))
        AppSortType.TargetSdk -> ApkAnalyzerIcons.Android to app.targetSdk.toString()
        AppSortType.LastUpdated -> ApkAnalyzerIcons.Calendar to app.lastUpdateTime.toShortDate(stringResource(R.string.preinstalled))
        AppSortType.LastUsed -> ApkAnalyzerIcons.Calendar to (app.lastUsedTime?.toShortDate(stringResource(R.string.never)) ?: stringResource(R.string.never))
        AppSortType.Name -> return
    }

    Row(
        modifier = modifier
            .background(AppTheme.colors.secondaryContainer, RoundedCornerShape(8.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.size(14.dp),
            tint = AppTheme.colors.onSecondaryContainer,
        )
        Text(
            text = value,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSecondaryContainer,
        )
    }
}

@Composable
private fun Instant.toShortDate(ifEmpty: String): String = if (this < Instant.ofEpochMilli(631152000000L)) {
    ifEmpty
} else {
    DateFormat.getDateInstance(DateFormat.SHORT, LocalLocale.current.platformLocale).format(Date(this.toEpochMilli()))
}
