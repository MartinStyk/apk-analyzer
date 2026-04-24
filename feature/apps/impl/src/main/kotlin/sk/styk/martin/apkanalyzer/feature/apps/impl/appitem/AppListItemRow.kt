package sk.styk.martin.apkanalyzer.feature.apps.impl.appitem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.components.AppIcon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Icon
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes
import sk.styk.martin.apkanalyzer.feature.apps.impl.list.AppListItem
import sk.styk.martin.apkanalyzer.feature.apps.impl.list.SortType
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AppListItemRow(
    app: AppListItem,
    onClick: () -> Unit,
    position: ListItemPosition,
    modifier: Modifier = Modifier,
    sortType: SortType = SortType.Name,
) {
    val shape: Shape =
        when (position) {
            ListItemPosition.Single -> Shapes.CardShape
            ListItemPosition.First -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ListItemPosition.Middle -> RectangleShape
            ListItemPosition.Last -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        }

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, shape)
            .padding(2.dp)
            .clip(Shapes.CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.packageName)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.applicationName,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = app.packageName,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (sortType != SortType.Name) {
            Spacer(modifier = Modifier.width(8.dp))
            SortValueBadge(app = app, sortType = sortType)
        }
    }
}

@Composable
private fun SortValueBadge(
    app: AppListItem,
    sortType: SortType,
    modifier: Modifier = Modifier,
) {
    val (icon, value) =
        when (sortType) {
            SortType.Size -> ApkAnalyzerIcons.FileUpload to app.apkSize.formatted()
            SortType.InstallDate -> ApkAnalyzerIcons.Calendar to app.installTime.toShortDate()
            SortType.TargetSdk -> ApkAnalyzerIcons.Android to app.targetSdk.toString()
            SortType.Name -> return
        }

    Row(
        modifier =
        modifier
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

private fun Long.toShortDate(): String = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(this))
